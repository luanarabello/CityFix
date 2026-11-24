package pt.isec.amov.cityfix.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.Visibility

@StringRes
fun TicketStatus.labelRes(): Int = when (this) {
    TicketStatus.ABERTO -> R.string.status_open
    TicketStatus.PEDIDO_DE_INFORMACOES -> R.string.status_info_requested
    TicketStatus.EM_RESOLUCAO -> R.string.status_in_progress
    TicketStatus.RESOLVIDO -> R.string.status_resolved
    TicketStatus.ENCERRADO -> R.string.status_closed
}

@StringRes
fun Situation.labelRes(): Int = when (this) {
    Situation.DANO -> R.string.situation_damage
    Situation.AVARIA -> R.string.situation_malfunction
    Situation.DESTRUICAO -> R.string.situation_destruction
    Situation.ROUBO -> R.string.situation_theft
}

@StringRes
fun Visibility.labelRes(): Int = when (this) {
    Visibility.PUBLIC -> R.string.visibility_public
    Visibility.PRIVATE -> R.string.visibility_private
}

@Composable
fun TicketStatus.displayLabel(): String = stringResource(labelRes())

@Composable
fun Situation.displayLabel(): String = stringResource(labelRes())

@Composable
fun Visibility.displayLabel(): String = stringResource(labelRes())

fun TicketStatus.displayLabel(context: Context): String = context.getString(labelRes())

fun Situation.displayLabel(context: Context): String = context.getString(labelRes())

fun Visibility.displayLabel(context: Context): String = context.getString(labelRes())
