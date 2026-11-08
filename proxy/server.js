// Minimal AI proxy: keeps the Gemini key off the phone. Node 18+.
// Env: GEMINI_API_KEY (required), APP_TOKEN (shared secret), GEMINI_MODEL (default below), PORT.
const http = require("http");
const MODEL = process.env.GEMINI_MODEL || "gemini-2.5-flash";
const SYSTEM =
  "You are Jarvis, a calm, witty personal assistant for a dentist in Kerala. " +
  "Reply in the user's language (English, Malayalam or Manglish). Spoken style, no markdown.";

http.createServer(async (req, res) => {
  const send = (code, obj) => { res.writeHead(code, { "Content-Type": "application/json" }); res.end(JSON.stringify(obj)); };
  if (req.method !== "POST" || req.url !== "/chat") return send(404, { error: "not found" });
  if (process.env.APP_TOKEN && req.headers.authorization !== `Bearer ${process.env.APP_TOKEN}`) return send(401, { error: "unauthorized" });
  let raw = "";
  for await (const c of req) raw += c;
  try {
    const { messages = [], driving = false } = JSON.parse(raw);
    const contents = messages.slice(-10).map(m => ({ role: m.role === "assistant" ? "model" : "user", parts: [{ text: String(m.text) }] }));
    const system = SYSTEM + (driving ? " The user is driving: answer in one short sentence." : " Keep answers under three sentences.");
    const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-goog-api-key": process.env.GEMINI_API_KEY },
      body: JSON.stringify({ systemInstruction: { parts: [{ text: system }] }, contents })
    });
    const j = await r.json();
    const reply = j.candidates?.[0]?.content?.parts?.map(p => p.text).join("") || "I have no answer for that.";
    send(200, { reply });
  } catch (e) { send(500, { error: "bad request" }); }
}).listen(process.env.PORT || 8080);
