import * as admin from "firebase-admin";
import {
    onRequest,
} from "firebase-functions/v2/https";
import {
    onDocumentCreated,
} from "firebase-functions/v2/firestore";

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// ─── Hilfsfunktionen ──────────────────────────────────────────────────────

async function sendToUser(
    uid: string,
    title: string,
    body: string,
    type: string
): Promise<void> {
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

async function sendToAll(
    title: string,
    body: string,
    type: string,
    excludeUid?: string
): Promise<void> {
    try {
        const snapshot = await db.collection("users").get();
        const sends = snapshot.docs
            .filter((doc) => doc.id !== excludeUid)
            .map((doc) => {
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

        await Promise.all(sends as Promise<string | null>[]);
    } catch (e) {
        console.error("sendToAll Fehler:", e);
    }
}

// ─── BAFSync Endpoint ──────────────────────────────────────────────────────

export const biersync = onRequest(
    { region: "europe-west3" },
    async (req, res) => {
        res.set("Access-Control-Allow-Origin", "*");
        res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
        res.set("Access-Control-Allow-Headers", "Content-Type");

        if (req.method === "OPTIONS") {
            res.status(204).send("");
            return;
        }
        if (req.method !== "POST") {
            res.status(405).send("Method not allowed");
            return;
        }

        try {
            const { token, uuid, name, bedrock } = req.body || {};

            if (!token || !uuid || !name) {
                res.status(400).json({ success: false, message: "Fehlende Felder" });
                return;
            }

            const tokenDoc = await db.collection("sync_tokens").doc(token).get();
            if (!tokenDoc.exists) {
                res.status(400).json({
                    success: false,
                    message: "Ungültiger Token. Bitte neu generieren."
                });
                return;
            }

            const tokenData = tokenDoc.data();
            const uid = tokenData?.uid;
            if (!uid) {
                res.status(400).json({ success: false, message: "Token ungültig" });
                return;
            }

            if (tokenData?.createdAt) {
                const createdAt = tokenData.createdAt.toMillis();
                if (Date.now() - createdAt > 10 * 60 * 1000) {
                    await tokenDoc.ref.delete();
                    res.status(400).json({
                        success: false,
                        message: "Token abgelaufen. Bitte neu generieren."
                    });
                    return;
                }
            }

            const userRef = db.collection("users").doc(uid);
            await userRef.set(
                { minecraftUuid: uuid, minecraftName: name, isBedrock: bedrock || false },
                { merge: true }
            );
            await tokenDoc.ref.delete();

            const userSnap = await userRef.get();
            const rank = userSnap.get("rank") || "malzbier";
            const username = userSnap.get("username") || name;

            await sendToUser(
                uid,
                "⚔ Minecraft verknüpft!",
                `Dein Account ${name} wurde erfolgreich verknüpft.`,
                "sync"
            );

            res.json({ success: true, message: "Sync erfolgreich", rank, username });
        } catch (e) {
            console.error("biersync Fehler:", e);
            res.status(500).json({ success: false, message: "Serverfehler" });
        }
    }
);

// ─── Neuer Chat-Beitrag ───────────────────────────────────────────────────

export const onNewChatMessage = onDocumentCreated(
    { document: "public_chat/{messageId}", region: "europe-west3" },
    async (event) => {
        const data = event.data?.data();
        if (!data) return;

        const authorName = data.authorName || "Jemand";
        const text = (data.text || "") as string;
        const authorUid = data.authorUid || "";

        await sendToAll(
            `💬 ${authorName} im Chat`,
            text.length > 80 ? text.substring(0, 80) + "..." : text,
            "chat",
            authorUid
        );
    }
);

// ─── Neue Privat-Nachricht ────────────────────────────────────────────────

export const onNewPrivateMessage = onDocumentCreated(
    {
        document: "private_chats/{chatId}/messages/{messageId}",
        region: "europe-west3"
    },
    async (event) => {
        const data = event.data?.data();
        if (!data) return;

        const senderName = data.senderName || "Jemand";
        const text = (data.text || "") as string;
        const receiverUid = data.receiverUid || "";

        if (!receiverUid) return;

        await sendToUser(
            receiverUid,
            `📩 Neue Nachricht von ${senderName}`,
            text.length > 80 ? text.substring(0, 80) + "..." : text,
            "chat"
        );
    }
);

// ─── Neues Ticket ─────────────────────────────────────────────────────────

export const onNewTicket = onDocumentCreated(
    { document: "tickets/{ticketId}", region: "europe-west3" },
    async (event) => {
        const data = event.data?.data();
        if (!data) return;

        const authorName = data.authorName || "Jemand";
        const title = data.title || "";

        const staffRanks = ["supporter", "moderator", "admin", "cheffe", "trainee"];
        const snapshot = await db.collection("users").get();

        const sends = snapshot.docs
            .filter((doc) =>
                staffRanks.includes((doc.get("rank") || "").toLowerCase())
            )
            .map((doc) => {
                const token = doc.get("fcmToken");
                if (!token) return null;
                return messaging.send({
                    token,
                    notification: {
                        title: `🎫 Neues Ticket von ${authorName}`,
                        body: title
                    },
                    data: { type: "ticket" },
                    android: {
                        priority: "high",
                        notification: { channelId: "baf_notifications" }
                    }
                }).catch(() => null);
            })
            .filter(Boolean);

        await Promise.all(sends as Promise<string | null>[]);
    }
);

// ─── Neue Ticket-Nachricht ────────────────────────────────────────────────

export const onNewTicketMessage = onDocumentCreated(
    {
        document: "tickets/{ticketId}/messages/{messageId}",
        region: "europe-west3"
    },
    async (event) => {
        const data = event.data?.data();
        if (!data) return;

        const authorName = data.authorName || "Jemand";
        const text = (data.text || "") as string;
        const authorUid = data.authorUid || "";
        const ticketId = event.params.ticketId;

        const ticketDoc = await db.collection("tickets").doc(ticketId).get();
        const ticketAuthorUid = ticketDoc.get("authorUid");

        if (ticketAuthorUid && ticketAuthorUid !== authorUid) {
            await sendToUser(
                ticketAuthorUid,
                "🎫 Antwort auf dein Ticket",
                `${authorName}: ${text.length > 60 ? text.substring(0, 60) + "..." : text}`,
                "ticket"
            );
        }
    }
);

// ─── Neuer Forum-Beitrag ──────────────────────────────────────────────────

export const onNewForumPost = onDocumentCreated(
    { document: "forum/{postId}", region: "europe-west3" },
    async (event) => {
        const data = event.data?.data();
        if (!data) return;

        const author = data.author || "Jemand";
        const title = data.title || "";
        const authorUid = data.authorUid || "";

        await sendToAll(
            `📋 Neuer Beitrag von ${author}`,
            title,
            "forum",
            authorUid
        );
    }
);

// ─── Neues Event ──────────────────────────────────────────────────────────

export const onNewEvent = onDocumentCreated(
    { document: "events/{eventId}", region: "europe-west3" },
    async (event) => {
        const data = event.data?.data();
        if (!data) return;

        const name = data.name || "Neues Event";
        const description = (data.description || "") as string;

        await sendToAll(
            `🎉 Neues Event: ${name}`,
            description.length > 80
                ? description.substring(0, 80) + "..."
                : description,
            "event"
        );
    }
);

// ─── Neues Markt-Angebot ──────────────────────────────────────────────────

export const onNewMarketItem = onDocumentCreated(
    { document: "market/{itemId}", region: "europe-west3" },
    async (event) => {
        const data = event.data?.data();
        if (!data) return;

        const ownerName = data.ownerName || "Jemand";
        const title = data.title || "";
        const ownerUuid = data.ownerUuid || "";

        await sendToAll(
            `🛒 Neues Angebot von ${ownerName}`,
            title,
            "market",
            ownerUuid
        );
    }
);
