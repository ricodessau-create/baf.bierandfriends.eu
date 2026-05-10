import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// ─── Hilfsfunktion: Token holen und Notification senden ───────────────────
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
export const biersync = functions.https.onRequest(async (req, res) => {
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
            res.status(400).json({ success: false, message: "Ungültiger Token. Bitte neu generieren." }); return;
        }

        const tokenData = tokenDoc.data();
        const uid = tokenData?.uid;
        if (!uid) {
            res.status(400).json({ success: false, message: "Token ungültig" }); return;
        }

        if (tokenData?.createdAt) {
            const createdAt = tokenData.createdAt.toMillis();
            if (Date.now() - createdAt > 10 * 60 * 1000) {
                await tokenDoc.ref.delete();
                res.status(400).json({ success: false, message: "Token abgelaufen. Bitte neu generieren." }); return;
            }
        }

        const userRef = db.collection("users").doc(uid);
        await userRef.set({ minecraftUuid: uuid, minecraftName: name, isBedrock: bedrock || false }, { merge: true });
        await tokenDoc.ref.delete();

        const userSnap = await userRef.get();
        const rank = userSnap.get("rank") || "malzbier";
        const username = userSnap.get("username") || name;

        // Notification an den User
        await sendToUser(uid, "⚔ Minecraft verknüpft!", `Dein Account ${name} wurde erfolgreich verknüpft.`, "sync");

        res.json({ success: true, message: "Sync erfolgreich", rank, username });
    } catch (e) {
        console.error("biersync Fehler:", e);
        res.status(500).json({ success: false, message: "Serverfehler" });
    }
});

// ─── Trigger: Neue Chat-Nachricht ─────────────────────────────────────────
export const onNewChatMessage = functions.firestore
    .document("public_chat/{messageId}")
    .onCreate(async (snap) => {
        const data = snap.data();
        const authorName = data?.authorName || "Jemand";
        const text = data?.text || "";
        const authorUid = data?.authorUid || "";

        await sendToAll(
            `💬 ${authorName} im Chat`,
            text.length > 80 ? text.substring(0, 80) + "..." : text,
            "chat",
            authorUid
        );
    });

// ─── Trigger: Neue Privat-Nachricht ───────────────────────────────────────
export const onNewPrivateMessage = functions.firestore
    .document("private_chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap) => {
        const data = snap.data();
        const senderName = data?.senderName || "Jemand";
        const text = data?.text || "";
        const receiverUid = data?.receiverUid || "";

        if (!receiverUid) return;

        await sendToUser(
            receiverUid,
            `📩 Neue Nachricht von ${senderName}`,
            text.length > 80 ? text.substring(0, 80) + "..." : text,
            "chat"
        );
    });

// ─── Trigger: Neues Ticket ────────────────────────────────────────────────
export const onNewTicket = functions.firestore
    .document("tickets/{ticketId}")
    .onCreate(async (snap) => {
        const data = snap.data();
        const authorName = data?.authorName || "Jemand";
        const title = data?.title || "";

        // Alle Staff-Mitglieder benachrichtigen
        const staffRanks = ["supporter", "moderator", "admin", "cheffe", "trainee"];
        const snapshot = await db.collection("users").get();

        const sends = snapshot.docs
            .filter(doc => staffRanks.includes((doc.get("rank") || "").toLowerCase()))
            .map(doc => {
                const token = doc.get("fcmToken");
                if (!token) return null;
                return messaging.send({
                    token,
                    notification: {
                        title: `🎫 Neues Ticket von ${authorName}`,
                        body: title
                    },
                    data: { type: "ticket" },
                    android: { priority: "high", notification: { channelId: "baf_notifications" } }
                }).catch(() => null);
            })
            .filter(Boolean);

        await Promise.all(sends as Promise<any>[]);
    });

// ─── Trigger: Ticket-Nachricht ────────────────────────────────────────────
export const onNewTicketMessage = functions.firestore
    .document("tickets/{ticketId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const data = snap.data();
        const authorName = data?.authorName || "Jemand";
        const text = data?.text || "";
        const authorUid = data?.authorUid || "";
        const ticketId = context.params.ticketId;

        // Ticket-Ersteller benachrichtigen
        const ticketDoc = await db.collection("tickets").doc(ticketId).get();
        const ticketAuthorUid = ticketDoc.get("authorUid");

        if (ticketAuthorUid && ticketAuthorUid !== authorUid) {
            await sendToUser(
                ticketAuthorUid,
                `🎫 Antwort auf dein Ticket`,
                `${authorName}: ${text.length > 60 ? text.substring(0, 60) + "..." : text}`,
                "ticket"
            );
        }
    });

// ─── Trigger: Neuer Forum-Beitrag ─────────────────────────────────────────
export const onNewForumPost = functions.firestore
    .document("forum/{postId}")
    .onCreate(async (snap) => {
        const data = snap.data();
        const author = data?.author || "Jemand";
        const title = data?.title || "";
        const authorUid = data?.authorUid || "";

        await sendToAll(
            `📋 Neuer Beitrag von ${author}`,
            title,
            "forum",
            authorUid
        );
    });

// ─── Trigger: Neues Event ─────────────────────────────────────────────────
export const onNewEvent = functions.firestore
    .document("events/{eventId}")
    .onCreate(async (snap) => {
        const data = snap.data();
        const name = data?.name || "Neues Event";
        const description = data?.description || "";

        await sendToAll(
            `🎉 Neues Event: ${name}`,
            description.length > 80 ? description.substring(0, 80) + "..." : description,
            "event"
        );
    });

// ─── Trigger: Neues Markt-Angebot ─────────────────────────────────────────
export const onNewMarketItem = functions.firestore
    .document("market/{itemId}")
    .onCreate(async (snap) => {
        const data = snap.data();
        const ownerName = data?.ownerName || "Jemand";
        const title = data?.title || "";
        const ownerUuid = data?.ownerUuid || "";

        await sendToAll(
            `🛒 Neues Angebot von ${ownerName}`,
            title,
            "market",
            ownerUuid
        );
    });
