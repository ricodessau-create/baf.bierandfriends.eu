import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

admin.initializeApp();
const db        = admin.firestore();
const messaging = admin.messaging();
const reg       = "us-central1";

/** Sendet eine Push-Nachricht an einen einzelnen User */
async function sendToUser(
    uid: string,
    title: string,
    body: string,
    type: string,
    extraData: Record<string, string> = {}
) {
    try {
        const doc   = await db.collection("users").doc(uid).get();
        const token = doc.get("fcmToken");
        if (!token) return;
        await messaging.send({
            token,
            notification: { title, body },
            data: { type, ...extraData },
            android: { priority: "high", notification: { channelId: "baf_notifications" } }
        });
    } catch (e) { console.error("sendToUser:", e); }
}

/** Sendet eine Push-Nachricht an alle User (außer excludeUid) */
async function sendToAll(
    title: string,
    body: string,
    type: string,
    excludeUid = "",
    extraData: Record<string, string> = {}
) {
    try {
        const snap = await db.collection("users").get();
        await Promise.all(
            snap.docs
                .filter(d => d.id !== excludeUid)
                .map(d => {
                    const token = d.get("fcmToken");
                    if (!token) return null;
                    return messaging.send({
                        token,
                        notification: { title, body },
                        data: { type, ...extraData },
                        android: { priority: "high", notification: { channelId: "baf_notifications" } }
                    }).catch(() => null);
                })
                .filter(Boolean) as Promise<any>[]
        );
    } catch (e) { console.error("sendToAll:", e); }
}

// Name geändert von biersync auf biersync_app wegen Google Cloud Konflikt
export const biersync_app = functions.region(reg).https.onRequest(async (req, res) => {
    res.set("Access-Control-Allow-Origin", "*");
    res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    res.set("Access-Control-Allow-Headers", "Content-Type");
    if (req.method === "OPTIONS") { res.status(204).send(""); return; }
    if (req.method !== "POST")   { res.status(405).send("Method not allowed"); return; }
    try {
        const { token, uuid, name, bedrock } = req.body || {};
        if (!token || !uuid || !name) {
            res.status(400).json({ success: false, message: "Fehlende Felder" }); return;
        }
        const tokenDoc = await db.collection("sync_tokens").doc(token).get();
        if (!tokenDoc.exists) {
            res.status(400).json({ success: false, message: "Ungültiger Token" }); return;
        }
        const uid = tokenDoc.data()?.uid;
        if (!uid) { res.status(400).json({ success: false, message: "Token ungültig" }); return; }
        if (tokenDoc.data()?.createdAt) {
            const created = tokenDoc.data()!.createdAt.toMillis();
            if (Date.now() - created > 10 * 60 * 1000) {
                await tokenDoc.ref.delete();
                res.status(400).json({ success: false, message: "Token abgelaufen" }); return;
            }
        }
        const userRef  = db.collection("users").doc(uid);
        await userRef.set({ minecraftUuid: uuid, minecraftName: name, isBedrock: !!bedrock }, { merge: true });
        await tokenDoc.ref.delete();
        const userSnap = await userRef.get();
        const rank     = userSnap.get("rank")     || "malzbier";
        const username = userSnap.get("username") || name;
        await sendToUser(uid, "⚔ Minecraft verknüpft!", `Account ${name} wurde verknüpft.`, "sync");
        res.json({ success: true, rank, username });
    } catch (e) { console.error("biersync:", e); res.status(500).json({ success: false }); }
});

// ─── Chat-Trigger ────────────────────────────────────────────────────────────

export const onNewPublicChat = functions.region(reg).firestore
    .document("public_chat/{id}").onCreate(async (snap) => {
        const d    = snap.data();
        const text = (d?.text || "") as string;
        await sendToAll(
            `💬 ${d?.authorName || "Jemand"}`,
            text.length > 80 ? text.substring(0, 80) + "..." : text,
            "chat",
            d?.authorUid || "",
            // chatType "public" → App kann Direct-Reply korrekt routen
            { chatType: "public" }
        );
    });

export const onNewPrivateMessage = functions.region(reg).firestore
    .document("private_chats/{chatId}/messages/{id}").onCreate(async (snap) => {
        const d    = snap.data();
        const text = (d?.text || "") as string;
        if (!d?.receiverUid) return;
        await sendToUser(
            d.receiverUid,
            `📩 ${d?.senderName || "Jemand"}`,
            text.length > 80 ? text.substring(0, 80) + "..." : text,
            "chat",
            // chatType "private" + senderUid → App kann Direct-Reply an Absender schicken
            { chatType: "private", senderUid: d.senderUid || "" }
        );
    });

// ─── Weitere Trigger ─────────────────────────────────────────────────────────

export const onNewTicket = functions.region(reg).firestore
    .document("tickets/{id}").onCreate(async (snap) => {
        const d          = snap.data();
        const staffRanks = ["supporter", "moderator", "admin", "cheffe", "trainee"];
        const users      = await db.collection("users").get();
        await Promise.all(
            users.docs
                .filter(u => staffRanks.includes((u.get("rank") || "").toLowerCase()))
                .map(u => {
                    const token = u.get("fcmToken");
                    if (!token) return null;
                    return messaging.send({
                        token,
                        notification: {
                            title: `🎫 Neues Ticket von ${d?.authorName || "Jemand"}`,
                            body:  d?.title || ""
                        },
                        data: { type: "ticket" },
                        android: { priority: "high", notification: { channelId: "baf_notifications" } }
                    }).catch(() => null);
                })
                .filter(Boolean) as Promise<any>[]
        );
    });

export const onNewTicketMessage = functions.region(reg).firestore
    .document("tickets/{ticketId}/messages/{id}").onCreate(async (snap, context) => {
        const d          = snap.data();
        const text       = (d?.text || "") as string;
        const ticketDoc  = await db.collection("tickets").doc(context.params.ticketId).get();
        const ownerUid   = ticketDoc.get("authorUid");
        if (ownerUid && ownerUid !== d?.authorUid) {
            await sendToUser(
                ownerUid,
                "🎫 Antwort auf dein Ticket",
                `${d?.authorName || "Jemand"}: ${text.length > 60 ? text.substring(0, 60) + "..." : text}`,
                "ticket"
            );
        }
    });

export const onNewForumPost = functions.region(reg).firestore
    .document("forum/{id}").onCreate(async (snap) => {
        const d = snap.data();
        await sendToAll(
            `📋 ${d?.author || "Jemand"} im Forum`,
            d?.title || "", "forum", d?.authorUid || ""
        );
    });

export const onNewEvent = functions.region(reg).firestore
    .document("events/{id}").onCreate(async (snap) => {
        const d    = snap.data();
        const desc = (d?.description || "") as string;
        await sendToAll(
            `🎉 Neues Event: ${d?.name || ""}`,
            desc.length > 80 ? desc.substring(0, 80) + "..." : desc,
            "event"
        );
    });

export const onNewMarketItem = functions.region(reg).firestore
    .document("market/{id}").onCreate(async (snap) => {
        const d = snap.data();
        await sendToAll(
            `🛒 ${d?.ownerName || "Jemand"} im Markt`,
            d?.title || "", "market", d?.ownerUuid || ""
        );
    });
