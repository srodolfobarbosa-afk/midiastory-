package com.rdf112.sweep.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdf112.sweep.presentation.ui.theme.MidiastoreColors

@Composable
fun ProScreen(
    state: ProUiState,
    onPurchaseRequested: (String) -> Unit,
    onRestorePurchases: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plans = if (state.plans.isNotEmpty()) {
        state.plans
    } else {
        listOf(
            ProPlanUiModel(
                productId = "sweep_pro_annual",
                title = "Pro Anual",
                priceLabel = "R$ 79,99/ano",
                description = "Melhor custo-benefício. Varredura global SHA-256 e limpeza em lote ilimitada.",
                isFeatured = true,
            ),
            ProPlanUiModel(
                productId = "sweep_pro_monthly",
                title = "Pro Mensal",
                priceLabel = "R$ 14,99/mês",
                description = "Acesso completo aos recursos Pro com flexibilidade mensal.",
            ),
            ProPlanUiModel(
                productId = "sweep_pro_lifetime",
                title = "Pro Vitalício",
                priceLabel = "R$ 149,99",
                description = "Pagamento único sem assinatura. Acesso permanente via Google Play Billing.",
            ),
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MidiastoreColors.ElevatedBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProMariaHeaderSection(
                isActive = true,
                isProActive = state.isProActive,
            )
        }

        items(plans, key = { it.productId }) { plan ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MidiastoreColors.CardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (plan.isFeatured) 1.5.dp else 1.dp,
                        color = if (plan.isFeatured) MidiastoreColors.AccentBlue else MidiastoreColors.CardStroke,
                        shape = RoundedCornerShape(14.dp),
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (plan.isFeatured) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MidiastoreColors.GreenSoft,
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            Text(
                                text = "MAIS ESCOLHIDO • ECONOMIZE 55%",
                                color = MidiastoreColors.GreenSegment,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = plan.title,
                            color = MidiastoreColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = plan.priceLabel,
                            color = MidiastoreColors.AccentBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = plan.description,
                        color = MidiastoreColors.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onPurchaseRequested(plan.productId) },
                        enabled = !state.isPurchasing && !state.isProActive,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (plan.isFeatured) MidiastoreColors.AccentBlue else MidiastoreColors.CardSurfaceAlt,
                            contentColor = if (plan.isFeatured) Color.White else MidiastoreColors.TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (state.isProActive) "Plano ativo" else "Continuar",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onRestorePurchases,
                enabled = !state.isPurchasing,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Restaurar compras",
                    color = MidiastoreColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item {
            ProGeminiNanoBottomCard()
        }

        item {
            Text(
                text = "Assinaturas renovam automaticamente até serem canceladas na Google Play Store. Nenhuma mídia é apagada sem sua confirmação no diálogo oficial do Android.",
                color = MidiastoreColors.TextSubtle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
            )
        }
    }
}
