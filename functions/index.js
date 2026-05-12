const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

async function sendToUser(uid, title, body, type) {
    try {
        const doc = await db.collection("users").doc(uid).get();
        const token = doc.get("fcmToken");
        if (!token) return;
        await messaging.send({
            token,
            notification: { title, body },
            data: { type },
            android: { priority: "high", notification: { channelId: "baf_notifications" } }
        });
    } catch(e) { console.error("sendToUser:", e); }
}

async function sendToAll(title, body, type, excludeUid) {
    try {
        const snap = await db.collection("users").get();
        await Promise.all(snap.docs
            .filter(d => d.id !== excludeUid)
            .map(d => {
                const token = d.get("fcmToken");
                if (!token) return null;
                return messaging.send({
                    token,
                    notification: { title, body },
                    data: { type },
                    android: { priority: "high", notification: { channelId: "baf_notifications" } }
                }).catch(() => null);
            }).filter(Boolean));
    } catch(e) { console.error("sendToAll:", e); }
}

exports.biersync = onRequest({ region: "us-central1" }, async (req, res) => {
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") { res.status(204).send(""); return; }
    if (req.method !== "POST") { res.status(405).send("Not allowed"); return; }
    try {
        const { token, uuid, name, bedrock } = req.body || {};
        if (!token || !uuid || !name) {
            res.status(400).json({ success: false, message: "Fehlende Felder" }); return;
        }
        const tokenDoc = await db.collection("sync_tokens").doc(token).get();
        if (!tokenDoc.exists) {
            res.status(400).json({ success: false, message: "Ungültiger Token" }); return;
        }
        const uid = tokenDoc.data().uid;
        if (!uid) {
            res.status(400).json({ success: false, message: "Token ungültig" }); return;
        }
        if (tokenDoc.data().createdAt) {
            if (Date.now() - tokenDoc.data().createdAt.toMillis() > 10 * 60 * 1000) {
                await tokenDoc.ref.delete();
                res.status(400).json({ success: false, message: "Token abgelaufen" }); return;
            }
        }
        const userRef = db.collection("users").doc(uid);
        await userRef.set(
            { minecraftUuid: uuid, minecraftName: name, isBedrock: !!bedrock },
            { merge: true }
        );
        await tokenDoc.ref.delete();
        const userSnap = await userRef.get();
        res.json({
            success: true,
            rank: userSnap.get("rank") || "malzbier",
            username: userSnap.get("username") || name
        });
    } catch(e) { console.error("biersync:", e); res.status(500).json({ success: false }); }
});

exports.onNewPublicChat = onDocumentCreated(
    { document: "public_chat/{id}", region: "us-central1" },
    async (event) => {
        const d = event.data.data();
        const text = d.text || "";
        await sendToAll(
            "💬 " + (d.authorName || "Jemand"),
            text.length > 80 ? text.substring(0, 80) + "..." : text,
            "chat", d.authorUid || ""
        );
    }
);

exports.onNewPrivateMessage = onDocumentCreated(
    { document: "private_chats/{chatId}/messages/{id}", region: "us-central1" },
    async (event) => {
        const d = event.data.data();
        if (!d.receiverUid) return;
        const text = d.text || "";
        await sendToUser(
            d.receiverUid,
            "📩 " + (d.senderName || "Jemand"),
            text.length > 80 ? text.substring(0, 80) + "..." : text,
            "chat"
        );
    }
);

exports.onNewTicket = onDocumentCreated(
    { document: "tickets/{id}", region: "us-central1" },
    async (event) => {
        const d = event.data.data();
        const staffRanks = ["supporter", "moderator", "admin", "cheffe", "trainee"];
        const users = await db.collection("users").get();
        await Promise.all(users.docs
            .filter(u => staffRanks.includes((u.get("rank") || "").toLowerCase()))
            .map(u => {
                const token = u.get("fcmToken");
                if (!token) return null;
                return messaging.send({
                    token,
                    notification: {
                        title: "🎫 Neues Ticket von " + (d.authorName || "Jemand"),
                        body: d.title || ""
                    },
                    data: { type: "ticket" },
                    android: { priority: "high", notification: { channelId: "baf_notifications" } }
                }).catch(() => null);
            }).filter(Boolean));
    }
);

exports.onNewTicketMessage = onDocumentCreated(
    { document: "tickets/{ticketId}/messages/{id}", region: "us-central1" },
    async (event) => {
        const d = event.data.data();
        const ticketDoc = await db.collection("tickets").doc(event.params.ticketId).get();
        const ownerUid = ticketDoc.get("authorUid");
        if (ownerUid && ownerUid !== d.authorUid) {
            const text = d.text || "";
            await sendToUser(
                ownerUid,
                "🎫 Antwort auf dein Ticket",
                (d.authorName || "Jemand") + ": " + (text.length > 60 ? text.substring(0, 60) + "..." : text),
                "ticket"
            );
        }
    }
);

exports.onNewForumPost = onDocumentCreated(
    { document: "forum/{id}", region: "us-central1" },
    async (event) => {
        const d = event.data.data();
        await sendToAll(
            "📋 " + (d.author || "Jemand") + " im Forum",
            d.title || "", "forum", d.authorUid || ""
        );
    }
);

exports.onNewEvent = onDocumentCreated(
    { document: "events/{id}", region: "us-central1" },
    async (event) => {
        const d = event.data.data();
        const desc = d.description || "";
        await sendToAll(
            "🎉 Neues Event: " + (d.name || ""),
            desc.length > 80 ? desc.substring(0, 80) + "..." : desc,
            "event", ""
        );
    }
);

exports.onNewMarketItem = onDocumentCreated(
    { document: "market/{id}", region: "us-central1" },
    async (event) => {
        const d = event.data.data();
        await sendToAll(
            "🛒 " + (d.ownerName || "Jemand") + " im Markt",
            d.title || "", "market", d.ownerUuid || ""
        );
    }
);
