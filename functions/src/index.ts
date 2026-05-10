import { onRequest } from "firebase-functions/v2/https";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// Region auf USA lassen, damit dein Link im Plugin exakt so bleibt wie er ist!
const opts = { region: "us-central1" };

// ─── ZENTRALE HILFSFUNKTIONEN (Sorgen dafür, dass es am Handy bimmelt) ───

async function sendPush(uid: string, title: string, body: string, type: string) {
    try {
        const userDoc = await db.collection("users").doc(uid).get();
        const token = userDoc.get("fcmToken");
        if (!token) return;

        await messaging.send({
            token,
            notification: { title, body },
            data: { type },
            android: {
                priority: "high",
                notification: { 
                    channelId: "baf_notifications", // WICHTIG: Muss exakt wie in deiner App.txt sein
                    sound: "default",
                    clickAction: "OPEN_ACTIVITY_1"
                }
            }
        });
    } catch (e) {
        console.error(`Push-Fehler bei User ${uid}:`, e);
    }
}

async function broadcastPush(title: string, body: string, type: string, excludeUid?: string) {
    try {
        const snapshot = await db.collection("users").get();
        const promises = snapshot.docs
            .filter(doc => doc.id !== excludeUid)
            .map(doc => {
                const token = doc.get("fcmToken");
                if (!token) return null;
                return messaging.send({
                    token,
                    notification: { title, body },
                    data: { type },
                    android: {
                        priority: "high",
                        notification: { channelId: "baf_notifications" }
                    }
                }).catch(() => null);
            });
        await Promise.all(promises);
    } catch (e) {
        console.error("Broadcast-Fehler:", e);
    }
}

// ─── 1. DER BIERSYNC ENDPOINT (Für dein Minecraft Plugin) ────────────────

export const biersync = onRequest(opts, async (req, res) => {
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") { res.status(204).send(""); return; }
    
    try {
        const { token, uuid, name, bedrock } = req.body || {};
        const tokenDoc = await db.collection("sync_tokens").doc(token).get();
        const uid = tokenDoc.data()?.uid;

        if (!uid) { res.status(400).send("Token ungültig"); return; }

        await db.collection("users").doc(uid).set({ 
            minecraftUuid: uuid, 
            minecraftName: name, 
            isBedrock: bedrock || false 
        }, { merge: true });

        await tokenDoc.ref.delete();
        await sendPush(uid, "⚔ Account verknüpft!", `Hi ${name}, dein Account ist jetzt startklar!`, "sync");
        res.json({ success: true });
    } catch (e) {
        res.status(500).send("Fehler");
    }
});

// ─── 2. CHAT PUSH (Wenn jemand im globalen Chat schreibt) ───────────────

export const onNewChatMessage = onDocumentCreated(opts, "public_chat/{messageId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    await broadcastPush(`💬 ${data.authorName || "Jemand"} im Chat`, data.text || "...", "chat", data.authorUid);
});

// ─── 3. PRIVAT-NACHRICHTEN PUSH ──────────────────────────────────────────

export const onNewPrivateMessage = onDocumentCreated(opts, "private_chats/{chatId}/messages/{messageId}", async (event) => {
    const data = event.data?.data();
    if (!data || !data.receiverUid) return;
    await sendPush(data.receiverUid, `📩 Nachricht von ${data.senderName}`, data.text || "...", "chat");
});

// ─── 4. TICKET PUSH (Neu für Staff / Antwort für User) ───────────────────

export const onNewTicket = onDocumentCreated(opts, "tickets/{ticketId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const staffRanks = ["supporter", "moderator", "admin", "cheffe", "trainee"];
    const snapshot = await db.collection("users").get();
    
    const staffSends = snapshot.docs
        .filter(doc => staffRanks.includes((doc.get("rank") || "").toLowerCase()))
        .map(doc => sendPush(doc.id, `🎫 Neues Ticket!`, `${data.authorName}: ${data.title}`, "ticket"));
    await Promise.all(staffSends);
});

export const onNewTicketMessage = onDocumentCreated(opts, "tickets/{ticketId}/messages/{messageId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const ticketDoc = await db.collection("tickets").doc(event.params.ticketId).get();
    const ticketAuthorUid = ticketDoc.get("authorUid");
    if (ticketAuthorUid && ticketAuthorUid !== data.authorUid) {
        await sendPush(ticketAuthorUid, `🎫 Ticket-Update`, `${data.authorName}: ${data.text}`, "ticket");
    }
});

// ─── 5. FORUM PUSH (Bei neuen Beiträgen) ──────────────────────────────────

export const onNewForumPost = onDocumentCreated(opts, "forum/{postId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    await broadcastPush(`📋 Forum: ${data.author}`, data.title || "Neuer Beitrag", "forum", data.authorUid);
});

// ─── 6. EVENT PUSH (Wenn ein Event erstellt wird) ─────────────────────────

export const onNewEvent = onDocumentCreated(opts, "events/{eventId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    await broadcastPush(`🎉 Neues Event!`, data.name || "Schau mal rein!", "event");
});

// ─── 7. MARKTPLATZ PUSH (Neue Items) ──────────────────────────────────────

export const onNewMarketItem = onDocumentCreated(opts, "market/{itemId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    await broadcastPush(`🛒 Markt: ${data.ownerName}`, data.title || "Neues Angebot", "market", data.ownerUuid);
});
