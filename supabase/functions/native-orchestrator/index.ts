import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

interface OrchestratorState {
  id: number;
  goal: string;
  status: "RUNNING" | "WAITING_HANDS" | "META_CONCLUIDA" | "PAUSED";
  step_count: number;
  max_steps: number;
  last_thought?: string;
  last_action?: string;
}

interface LlmDecision {
  thought: string;
  action: "DISPATCH_CLOUD_HANDS" | "DELEGATE_LOCAL_HANDS" | "EVALUATE_PROGRESS" | "META_CONCLUIDA";
  task?: {
    title: string;
    target: "CLOUD_ACTIONS" | "LOCAL_PC";
    script_or_command: string;
    parameters?: Record<string, unknown>;
  };
  meta_concluida: boolean;
  summary: string;
}

function getSupabaseClient() {
  const url = Deno.env.get("SUPABASE_URL") || "";
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
  return createClient(url, key);
}

async function callBrainLlm(prompt: string, state: OrchestratorState, recentLogs: unknown[], recentTasks: unknown[]): Promise<LlmDecision> {
  const openRouterKey = Deno.env.get("OPENROUTER_API_KEY");
  const googleStudioKey = Deno.env.get("GOOGLE_AI_STUDIO_API_KEY") || Deno.env.get("GOOGLE_AQ_API_KEY");
  const openRouterUrl = Deno.env.get("OPENROUTER_BASE_URL") || "https://openrouter.ai/api/v1/chat/completions";

  const systemPrompt = `Você é a IA Nativa Orquestradora do projeto Midiastore (Android App com.rdf112.midiastore).
Seu objetivo é controlar o loop autônomo de decisão na nuvem 24/7 até que o objetivo ativo seja totalmente alcançado.
Você aciona as "Super IAs" e as "Mãos":
- Mãos na Nuvem (GitHub Actions): executam scripts de automação (ex: "invisible-loop", "organic-growth", "check-google-apis", "verify-tests") 24h sem gastar recursos locais.
- Mãos no PC Local: executam compilações pesadas Gradle/Android Studio e testes com emulador/ADB quando o PC do usuário estiver conectado.

Instruções Cruciais:
1. Avalie o objetivo atual, os passos já executados e o status das tarefas recentes.
2. Se ainda restam ações a serem executadas, escolha a ação apropriada:
   - "DISPATCH_CLOUD_HANDS": para disparar execução no GitHub Actions (disponível 24/7 na nuvem).
   - "DELEGATE_LOCAL_HANDS": para agendar ação no PC local do desenvolvedor.
   - "EVALUATE_PROGRESS": para reavaliar dados e atualizar o plano.
3. Se e SOMENTE SE todo o objetivo foi comprovadamente atingido com sucesso, emita "action": "META_CONCLUIDA", "meta_concluida": true e inclua a tag [META_CONCLUIDA] no campo "summary".
4. Responda SEMPRE em JSON rigoroso seguindo o schema abaixo:
{
  "thought": "análise detalhada e raciocínio deste ciclo",
  "action": "DISPATCH_CLOUD_HANDS" | "DELEGATE_LOCAL_HANDS" | "EVALUATE_PROGRESS" | "META_CONCLUIDA",
  "task": {
    "title": "título da tarefa",
    "target": "CLOUD_ACTIONS" | "LOCAL_PC",
    "script_or_command": "invisible-loop" | "organic-growth" | "check-google-apis" | "verify-tests" | "custom",
    "parameters": {}
  },
  "meta_concluida": false (ou true se terminou),
  "summary": "resumo sucinto do passo atual ou [META_CONCLUIDA] final"
}`;

  const userContext = JSON.stringify({
    active_goal: state.goal,
    current_step: state.step_count,
    max_steps: state.max_steps,
    current_status: state.status,
    recent_tasks: recentTasks,
    recent_logs: recentLogs,
    prompt_instruction: prompt || "Execute o próximo ciclo de raciocínio e ação para o objetivo ativo.",
  });

  // 1. Rota Primária Oficial Gratuita: Google AI Studio (Gemini 4 Hydrogen - 1M Context + Thinking / Gemini 2.5 Flash)
  if (googleStudioKey) {
    for (const studioModel of ["gemini-4-hydrogen", "gemini-2.5-flash"]) {
      try {
        const gRes = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${studioModel}:generateContent?key=${googleStudioKey}`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              contents: [{ parts: [{ text: `${systemPrompt}\n\nCONTEXTO ATUAL:\n${userContext}` }] }],
              generationConfig: {
                responseMimeType: "application/json",
                temperature: 0.2,
                ...(studioModel.includes("gemini-4") ? { thinkingConfig: { thinkingLevel: "MEDIUM" } } : {}),
              },
            }),
          },
        );
        if (gRes.ok) {
          const gData = await gRes.json();
          const text = (gData.candidates?.[0]?.content?.parts || [])
            .map((p: { text?: string }) => p.text || "")
            .join("")
            .trim();
          if (text) {
            return JSON.parse(text) as LlmDecision;
          }
        }
      } catch (err) {
        console.warn(`Google AI Studio (${studioModel}) error:`, err);
      }
    }
  }

  // 2. Fallback OpenRouter (Modelos Gratuitos Ativos + Calibragem de max_tokens)
  const modelsToTry = [
    "nvidia/nemotron-3.5-lightning:free",
    "google/gemma-4-31b-it:free",
    "baidu/ernie-5.5-flash-preview:free",
    "openrouter/free",
  ];

  for (const model of modelsToTry) {
    try {
      const res = await fetch(openRouterUrl, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${openRouterKey}`,
          "Content-Type": "application/json",
          "HTTP-Referer": "https://midiastory-df6a0.web.app/",
          "X-Title": "Midiastory-Native-Orchestrator",
        },
        body: JSON.stringify({
          model,
          max_tokens: 1200,
          response_format: { type: "json_object" },
          messages: [
            { role: "system", content: systemPrompt },
            { role: "user", content: userContext },
          ],
          temperature: 0.2,
        }),
      });

      if (!res.ok) {
        console.warn(`Model ${model} returned ${res.status}: ${await res.text()}`);
        continue;
      }

      const data = await res.json();
      const content = data.choices?.[0]?.message?.content;
      if (content) {
        const parsed = JSON.parse(content) as LlmDecision;
        return parsed;
      }
    } catch (err) {
      console.warn(`Error trying model ${model}:`, err);
    }
  }

  throw new Error("Não foi possível obter resposta de nenhum modelo de IA.");
}

async function triggerGitHubHands(taskTitle: string, scriptOrCommand: string, taskId: number, stateId: number, params?: Record<string, unknown>) {
  const ghToken = Deno.env.get("GITHUB_TOKEN") || Deno.env.get("GH_TOKEN");
  if (!ghToken) {
    console.warn("GITHUB_TOKEN não configurado. Não foi possível disparar GitHub Actions.");
    return { dispatched: false, reason: "missing_token" };
  }

  const res = await fetch("https://api.github.com/repos/srodolfobarbosa-afk/midiastory-/dispatches", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${ghToken}`,
      "Accept": "application/vnd.github.v3+json",
      "User-Agent": "Midiastory-Native-Orchestrator",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      event_type: "orchestrator-cloud-task",
      client_payload: {
        task_id: taskId,
        state_id: stateId,
        title: taskTitle,
        script_or_command: scriptOrCommand,
        parameters: params || {},
      },
    }),
  });

  if (!res.ok) {
    const errorText = await res.text();
    console.error(`GitHub Actions dispatch error ${res.status}:`, errorText);
    return { dispatched: false, error: errorText };
  }

  return { dispatched: true };
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const supabase = getSupabaseClient();

  try {
    let body: Record<string, unknown> = {};
    if (req.method === "POST") {
      try {
        body = await req.json();
      } catch {
        body = {};
      }
    }

    const requestedAction = (body.action as string) || "STATUS";

    // 1. GET ou STATUS: Retorna estado atual
    if (req.method === "GET" || requestedAction === "STATUS") {
      const { data: state } = await supabase
        .from("orchestrator_state")
        .select("*")
        .order("id", { ascending: false })
        .limit(1)
        .maybeSingle();

      const { data: tasks } = await supabase
        .from("orchestrator_tasks")
        .select("*")
        .order("id", { ascending: false })
        .limit(10);

      const { data: logs } = await supabase
        .from("orchestrator_logs")
        .select("*")
        .order("id", { ascending: false })
        .limit(5);

      return new Response(
        JSON.stringify({
          orchestrator: "IA Nativa Orquestradora 24/7 (Supabase + GitHub Cloud Hands)",
          status: "ONLINE",
          state,
          recent_tasks: tasks,
          recent_logs: logs,
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 2. START: Inicializa um novo loop com objetivo
    if (requestedAction === "START") {
      const goal = (body.goal as string) || "Operação autônoma de otimização orgânica, monitoramento e reviews do Midiastore";
      const maxSteps = (body.max_steps as number) || 30;

      const { data: newState, error } = await supabase
        .from("orchestrator_state")
        .insert({
          goal,
          status: "RUNNING",
          step_count: 0,
          max_steps: maxSteps,
          last_thought: "Inicializando loop autônomo da IA Nativa Orquestradora 24/7.",
          last_action: "LOOP_STARTED",
        })
        .select()
        .single();

      if (error) throw error;

      // Grava no histórico de memórias universais
      await supabase.from("memories").insert({
        content: `[IA Nativa Orquestradora] Novo objetivo iniciado (#${newState.id}): "${goal}" (Limite: ${maxSteps} passos)`,
      });

      return new Response(
        JSON.stringify({ message: "Loop iniciado com sucesso!", state: newState }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. TASK_CALLBACK: Recebe resultado de uma Mão (GitHub Actions ou PC Local)
    if (requestedAction === "TASK_CALLBACK") {
      const taskId = body.task_id as number;
      const taskStatus = (body.status as string) || "DONE";
      const result = (body.result as Record<string, unknown>) || {};

      if (taskId) {
        await supabase
          .from("orchestrator_tasks")
          .update({
            status: taskStatus,
            result,
            updated_at: new Date().toISOString(),
          })
          .eq("id", taskId);
      }

      // Desbloqueia o estado para continuar o loop
      const stateId = body.state_id as number;
      if (stateId) {
        await supabase
          .from("orchestrator_state")
          .update({ status: "RUNNING", updated_at: new Date().toISOString() })
          .eq("id", stateId);
      }

      return new Response(
        JSON.stringify({ message: `Tarefa #${taskId} atualizada para ${taskStatus}. Estado liberado para próximo passo.` }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. STEP / RUN_LOOP: Ciclo de Raciocínio & Execução
    if (requestedAction === "STEP" || requestedAction === "RUN_LOOP") {
      const { data: state, error: stateError } = await supabase
        .from("orchestrator_state")
        .select("*")
        .order("id", { ascending: false })
        .limit(1)
        .maybeSingle();

      if (stateError) throw stateError;

      if (!state) {
        return new Response(
          JSON.stringify({ message: "Nenhum loop ativo. Envie action: 'START' com um objetivo." }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      if (state.status === "META_CONCLUIDA") {
        return new Response(
          JSON.stringify({
            message: "O objetivo já foi alcançado [META_CONCLUIDA]!",
            state,
          }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      // Se houver tarefas da nuvem rodando e o estado for WAITING_HANDS, verificar timeout ou aguardar
      const { data: recentTasks } = await supabase
        .from("orchestrator_tasks")
        .select("*")
        .eq("state_id", state.id)
        .order("id", { ascending: false })
        .limit(5);

      const hasPendingCloudTask = recentTasks?.some(
        (t) => t.status === "RUNNING" && t.target === "CLOUD_ACTIONS"
      );

      if (hasPendingCloudTask && requestedAction !== "STEP") {
        return new Response(
          JSON.stringify({
            message: "Aguardando conclusão das Mãos na Nuvem (GitHub Actions).",
            state,
            running_tasks: recentTasks?.filter((t) => t.status === "RUNNING"),
          }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      // Verifica limite de passos
      if (state.step_count >= state.max_steps) {
        await supabase
          .from("orchestrator_state")
          .update({
            status: "PAUSED",
            last_thought: `Limite máximo de ${state.max_steps} passos atingido. Loop pausado para revisão.`,
          })
          .eq("id", state.id);

        return new Response(
          JSON.stringify({ message: "Limite de passos atingido. Loop pausado.", state }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const { data: recentLogs } = await supabase
        .from("orchestrator_logs")
        .select("*")
        .eq("state_id", state.id)
        .order("id", { ascending: false })
        .limit(5);

      // Invoca Cérebro da IA Orquestradora
      const currentStep = state.step_count + 1;
      const decision = await callBrainLlm(
        (body.prompt as string) || "Próximo passo do loop autônomo.",
        state,
        recentLogs || [],
        recentTasks || []
      );

      // Grava no log
      await supabase.from("orchestrator_logs").insert({
        state_id: state.id,
        step: currentStep,
        thought: decision.thought,
        action: decision.action,
        raw_model_response: decision,
      });

      let nextStatus = "RUNNING";
      let dispatchedInfo = null;

      // Executa ação decidida
      if (decision.action === "DISPATCH_CLOUD_HANDS" && decision.task) {
        const { data: createdTask } = await supabase
          .from("orchestrator_tasks")
          .insert({
            state_id: state.id,
            title: decision.task.title,
            target: "CLOUD_ACTIONS",
            payload: {
              script_or_command: decision.task.script_or_command,
              parameters: decision.task.parameters,
            },
            status: "RUNNING",
          })
          .select()
          .single();

        if (createdTask) {
          dispatchedInfo = await triggerGitHubHands(
            decision.task.title,
            decision.task.script_or_command,
            createdTask.id,
            state.id,
            decision.task.parameters
          );
        }
        nextStatus = "WAITING_HANDS";
      } else if (decision.action === "DELEGATE_LOCAL_HANDS" && decision.task) {
        await supabase.from("orchestrator_tasks").insert({
          state_id: state.id,
          title: decision.task.title,
          target: "LOCAL_PC",
          payload: {
            script_or_command: decision.task.script_or_command,
            parameters: decision.task.parameters,
          },
          status: "PENDING",
        });
        nextStatus = "RUNNING";
      } else if (decision.meta_concluida || decision.action === "META_CONCLUIDA") {
        nextStatus = "META_CONCLUIDA";
        // Grava nas memórias universais
        await supabase.from("memories").insert({
          content: `[META_CONCLUIDA] Objetivo #${state.id} finalizado com sucesso!\nResumo: ${decision.summary}`,
        });
      }

      // Atualiza estado
      const { data: updatedState } = await supabase
        .from("orchestrator_state")
        .update({
          step_count: currentStep,
          status: nextStatus,
          last_thought: decision.thought,
          last_action: decision.action,
          updated_at: new Date().toISOString(),
        })
        .eq("id", state.id)
        .select()
        .single();

      return new Response(
        JSON.stringify({
          step: currentStep,
          decision,
          dispatched: dispatchedInfo,
          state: updatedState,
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({ error: `Ação desconhecida: ${requestedAction}` }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    console.error("Erro na Edge Function:", message);
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
