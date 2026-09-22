package id.rona.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Consistent section heading: optional title + trailing action + content. */
@Composable
fun RunaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
        if (supporting != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Section wrapper: header + spacing + content. */
@Composable
fun RunaSection(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth(),
    ) {
        RunaSectionHeader(title = title, supporting = supporting, trailing = trailing)
        Spacer(Modifier.height(12.dp))
        Column(content = content)
    }
}
