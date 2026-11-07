import { NextResponse } from "next/server";
import { createAdminClient } from "@supabase/server/core";

export const runtime = "nodejs";

async function gemini(model: string, contents: unknown[]) {
  const key = process.env.GEMINI_API_KEY;
  if (!key) throw new Error("Configure GEMINI_API_KEY no arquivo .env.local.");
  const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(key)}`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ contents }),
  });
  if (!response.ok) throw new Error("O serviço Gemini não aceitou a solicitação.");
  return response.json();
}

async function embed(text: string) {
  const key = process.env.GEMINI_API_KEY;
  if (!key) throw new Error("Configure GEMINI_API_KEY no arquivo .env.local.");
  const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=${encodeURIComponent(key)}`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ content: { parts: [{ text }] }, taskType: "RETRIEVAL_QUERY", outputDimensionality: 1536 }),
  });
  if (!response.ok) throw new Error("O serviço Gemini não conseguiu processar a pergunta.");
  return response.json();
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const question = typeof body.question === "string" ? body.question.trim().slice(0, 8000) : "";
    if (!question) return NextResponse.json({ error: "Escreva uma pergunta." }, { status: 400 });
    if (!process.env.SUPABASE_URL || !process.env.SUPABASE_SECRET_KEY) return NextResponse.json({ error: "Configure o acesso seguro ao Supabase no .env.local." }, { status: 503 });
    const supabase = createAdminClient();
    const embeddingResult = await embed(question);
    const embedding = embeddingResult.embedding?.values;
    if (!Array.isArray(embedding) || embedding.length !== 1536) throw new Error("Não foi possível criar o embedding da pergunta.");
    const { data: matches, error: searchError } = await supabase.rpc("match_documents", { query_embedding: embedding, match_count: 5 });
    if (searchError) throw new Error("A busca de documentos não está configurada no Supabase.");
    const { data: memories, error: memoryError } = await supabase.from("memories").select("content").order("created_at", { ascending: false }).limit(5);
    if (memoryError) throw new Error("A memória do Supabase não está configurada.");
    const context = (matches ?? []).map((row: { content: string }) => row.content).join("\n\n");
    const memoryContext = (memories ?? []).map((row: { content: string }) => row.content).join("\n\n");
    const answerResult = await gemini("gemini-2.0-flash", [{ role: "user", parts: [{ text: `Responda em português do Brasil usando os documentos e a memória quando ajudarem. Se não houver informação suficiente, diga isso claramente.\n\nDocumentos:\n${context || "Nenhum documento relevante encontrado."}\n\nMemória recente:\n${memoryContext || "Nenhuma memória salva."}\n\nPergunta: ${question}` }] }]);
    const answer = answerResult.candidates?.[0]?.content?.parts?.map((part: { text?: string }) => part.text ?? "").join("").trim();
    if (!answer) throw new Error("O Gemini retornou uma resposta vazia.");
    const { error: saveError } = await supabase.from("conversations").insert({ question, answer });
    if (saveError) throw new Error("A resposta foi gerada, mas não foi possível salvá-la.");
    await supabase.from("memories").insert({ content: `Pergunta: ${question}\nResposta: ${answer}`.slice(0, 8000) });
    return NextResponse.json({ answer });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Erro interno.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
