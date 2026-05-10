import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

// Region festlegen, damit die URL für das Plugin bleibt
const region = "us-central1";

// --- HILFSFUNKTION FÜR PUSH ---
async function sendPush(uid: string, title: string, body: string, type: string) {
    const userDoc = await db.collection("users").doc(uid).get();
    const token = userDoc.get("fcmToken");
    if (!token) return;
    await admin.messaging().send({
        token: token,
        notification: { title, body },
        data: { type },
        android: { notification: { channelId: "baf_notifications", sound: "default" } }
    }).catch(e => console.log("Push Error", e));
}

// --- BIERSYNC ENDPOINT ---
export const biersync = functions.region(region).https.onRequest(async (req, res) => {
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") { res.status(204).send(""); return; }
    const { token, uuid, name, bedrock } = req.body || {};
    const tokenDoc = await db.collection("sync_tokens").doc(token).get();
    const uid = tokenDoc.data()?.uid;
    if (!uid) { res.status(400).send("Token ungültig"); return; }
    await db.collection("users").doc(uid).set({ minecraftUuid: uuid, minecraftName: name, isBedrock: bedrock || false }, { merge: true });
    await tokenDoc.ref.delete();
    await sendPush(uid, "⚔ Account verknüpft!", `Hi ${name}, dein Account ist bereit!`, "sync");
    res.json({ success: true });
});

// --- PUSH TRIGGER ---

// Privatnachrichten
export const onNewPrivateMessage = functions.region(region).firestore.document("private_chats/{chatId}/messages/{messageId}").onCreate(async (snap) => {
    const data = snap.data();
    if (!data.receiverUid) return;
    await sendPush(data.receiverUid, `📩 Nachricht von ${data.senderName}`, data.text || "...", "chat");
});

// Öffentlicher Chat
export const onNewChatMessage = functions.region(region).firestore.document("public_chat/{messageId}").onCreate(async (snap) => {
    const data = snap.data();
    const users = await db.collection("users").get();
    const sends = users.docs.filter(d => d.id !== data.authorUid).map(d => sendPush(d.id, `💬 ${data.authorName}`, data.text || "...", "chat"));
    await Promise.all(sends);
});

// Tickets (für Staff)
export const onNewTicket = functions.region(region).firestore.document("tickets/{ticketId}").onCreate(async (snap) => {
    const data = snap.data();
    const staffRanks = ["supporter", "moderator", "admin", "cheffe", "trainee"];
    const users = await db.collection("users").get();
    const sends = users.docs.filter(d => staffRanks.includes((d.get("rank") || "").toLowerCase())).map(d => sendPush(d.id, "🎫 Neues Ticket", `${data.authorName}: ${data.title}`, "ticket"));
    await Promise.all(sends);
});

// Ticket Antworten (für User)
export const onNewTicketMessage = functions.region(region).firestore.document("tickets/{ticketId}/messages/{messageId}").onCreate(async (snap, context) => {
    const data = snap.data();
    const ticket = await db.collection("tickets").doc(context.params.ticketId).get();
    const authorUid = ticket.get("authorUid");
    if (authorUid && authorUid !== data.authorUid) {
        await sendPush(authorUid, "🎫 Ticket Update", `${data.authorName}: ${data.text}`, "ticket");
    }
});

// Events, Forum, Markt (Broadcast an alle)
const broadcastCollections = ["events", "forum", "market"];
broadcastCollections.forEach(col => {
    export const [`onNew${col.charAt(0).toUpperCase() + col.slice(1)}`] = functions.region(region).firestore.document(`${col}/{id}`).onCreate(async (snap) => {
        const data = snap.data();
        const users = await db.collection("users").get();
        const title = col === "events" ? "🎉 Neues Event" : col === "forum" ? "📋 Forum" : "🛒 Markt";
        const body = data.name || data.title || "Schau mal rein!";
        const sends = users.docs.map(d => sendPush(d.id, title, body, col));
        await Promise.all(sends);
    });
});
