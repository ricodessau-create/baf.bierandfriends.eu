import { onRequest } from "firebase-functions/v2/https";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// Konfiguration: Region festlegen (Frankfurt)
const opts = { region: "europe-west3" };

// ─── Hilfsfunktionen ───────────────────────────────────────────────────

async function sendToUser(uid: string, title: string, body: string, type: string) {
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
                notification: { channelId: "baf_notifications", sound: "default" }
            }
        });
    } catch (e) {
        console.error(`sendToUser(${uid}) Fehler:`, e);
    }
}

async function sendToAll(title: string, body: string, type: string, excludeUid?: string) {
    try {
        const snapshot = await db.collection("users").get();
        const sends = snapshot.docs
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
                        notification: { channelId: "baf_notifications", sound: "default" }
                    }
                }).catch(() => null);
            })
            .filter(Boolean);
        await Promise.all(sends as Promise<any>[]);
    } catch (e) {
        console.error("sendToAll Fehler:", e);
    }
}

// ─── BAFSync Endpoint ──────────────────────────────────────────────────────

export const biersync = onRequest(opts, async (req, res) => {
    res.set("Access-Control-Allow-Origin", "*");
    res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    res.set("Access-Control-Allow-Headers", "Content-Type");

    if (req.method === "OPTIONS") { res.status(204).send(""); return; }
    if (req.method !== "POST") { res.status(405).send("Method not allowed"); return; }

    try {
        const { token, uuid, name, bedrock } = req.body || {};
        if (!token || !uuid || !name) {
            res.status(400).json({ success: false, message: "Fehlende Felder" }); return;
        }

        const tokenDoc = await db.collection("sync_tokens").doc(token).get();
        if (!tokenDoc.exists) {
            res.status(400).json({ success: false, message: "Ungültiger Token." }); return;
        }

        const tokenData = tokenDoc.data();
        const uid = tokenData?.uid;
        if (!uid) {
            res.status(400).json({ success: false, message: "Token ungültig" }); return;
        }

        const userRef = db.collection("users").doc(uid);
        await userRef.set({ minecraftUuid: uuid, minecraftName: name, isBedrock: bedrock || false }, { merge: true });
        await tokenDoc.ref.delete();

        const userSnap = await userRef.get();
        const rank = userSnap.get("rank") || "malzbier";
        const username = userSnap.get("username") || name;

        await sendToUser(uid, "⚔ Minecraft verknüpft!", `Dein Account ${name} wurde verknüpft.`, "sync");

        res.json({ success: true, message: "Sync erfolgreich", rank, username });
    } catch (e) {
        console.error("biersync Fehler:", e);
        res.status(500).json({ success: false, message: "Serverfehler" });
    }
});

// ─── Firestore Trigger (v2) ───────────────────────────────────────────────

export const onNewChatMessage = onDocumentCreated(opts, "public_chat/{messageId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    await sendToAll(
        `💬 ${data.authorName || "Jemand"} im Chat`,
        (data.text || "").length > 80 ? data.text.substring(0, 80) + "..." : data.text,
        "chat",
        data.authorUid
    );
});

export const onNewPrivateMessage = onDocumentCreated(opts, "private_chats/{chatId}/messages/{messageId}", async (event) => {
    const data = event.data?.data();
    if (!data || !data.receiverUid) return;
    await sendToUser(
        data.receiverUid,
        `📩 Neue Nachricht von ${data.senderName || "Jemand"}`,
        (data.text || "").length > 80 ? data.text.substring(0, 80) + "..." : data.text,
        "chat"
    );
});

export const onNewTicket = onDocumentCreated(opts, "tickets/{ticketId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const staffRanks = ["supporter", "moderator", "admin", "cheffe", "trainee"];
    const snapshot = await db.collection("users").get();
    const sends = snapshot.docs
        .filter(doc => staffRanks.includes((doc.get("rank") || "").toLowerCase()))
        .map(doc => {
            const token = doc.get("fcmToken");
            if (!token) return null;
            return messaging.send({
                token,
                notification: { title: `🎫 Ticket von ${data.authorName || "Jemand"}`, body: data.title || "" },
                data: { type: "ticket" }
            }).catch(() => null);
        })
        .filter(Boolean);
    await Promise.all(sends);
});

export const onNewTicketMessage = onDocumentCreated(opts, "tickets/{ticketId}/messages/{messageId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const ticketDoc = await db.collection("tickets").doc(event.params.ticketId).get();
    const ticketAuthorUid = ticketDoc.get("authorUid");
    if (ticketAuthorUid && ticketAuthorUid !== data.authorUid) {
        await sendToUser(ticketAuthorUid, `🎫 Antwort auf dein Ticket`, `${data.authorName || "Jemand"}: ${(data.text || "").substring(0, 60)}...`, "ticket");
    }
});

export const onNewForumPost = onDocumentCreated(opts, "forum/{postId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    await sendToAll(`📋 Neuer Beitrag von ${data.author || "Jemand"}`, data.title || "", "forum", data.authorUid);
});

export const onNewEvent = onDocumentCreated(opts, "events/{eventId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    await sendToAll(`🎉 Neues Event: ${data.name || "Event"}`, (data.description || "").substring(0, 80) + "...", "event");
});

export const onNewMarketItem = onDocumentCreated(opts, "market/{itemId}", async (event) => {
    const data = event.data?.data();
    if (!data) return;
    await sendToAll(`🛒 Neues Angebot von ${data.ownerName || "Jemand"}`, data.title || "", "market", data.ownerUuid);
});
