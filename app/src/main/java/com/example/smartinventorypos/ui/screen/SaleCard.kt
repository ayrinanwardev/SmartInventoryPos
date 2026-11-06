package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.model.SaleRecord
import com.example.smartinventorypos.ui.components.SoftCard
import com.example.smartinventorypos.ui.displayDate
import com.example.smartinventorypos.ui.money
import com.example.smartinventorypos.ui.theme.Hairline
import com.example.smartinventorypos.ui.theme.Muted

/** One sale or order, used by both the sales report and the customer's order history. */
@Composable
fun SaleCard(title: String, sale: SaleRecord, byline: String? = null) {
    SoftCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(byline, displayDate(sale.createdAt))
                            .joinToString(stringResource(R.string.date_separator)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
                Text(money(sale.total), style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(color = Hairline)
            sale.lines.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        line.productName ?: stringResource(R.string.unknown_product),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.sale_line, line.quantity, money(line.unitPrice)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }
            if (sale.discount.isPositive) {
                Text(
                    stringResource(R.string.discount_amount, money(sale.discount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
    }
}
