package org.fdroid.fdroid.views.repos

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.os.LocaleListCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.fdroid.database.MinimalApp
import org.fdroid.database.Repository
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.Utils
import org.fdroid.fdroid.Utils.getGlideModel
import org.fdroid.fdroid.compose.ComposeUtils.FDroidButton
import org.fdroid.fdroid.compose.ComposeUtils.FDroidContent
import org.fdroid.index.v2.FileV2
import org.fdroid.repo.FetchResult.IsExistingRepository
import org.fdroid.repo.FetchResult.IsNewMirror
import org.fdroid.repo.FetchResult.IsNewRepository
import org.fdroid.repo.Fetching

@Composable
fun RepoPreviewScreen(paddingValues: PaddingValues, state: Fetching, onAddRepo: () -> Unit) {
    val localeList = LocaleListCompat.getDefault()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = spacedBy(8.dp),
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxWidth(),
    ) {
        item {
            RepoPreviewHeader(state, onAddRepo, localeList)
        }
        if (state.fetchResult == null || state.fetchResult is IsNewRepository) {
            item {
                Row(
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.repo_preview_included_apps),
                        style = MaterialTheme.typography.body1,
                    )
                    Text(
                        text = state.apps.size.toString(),
                        style = MaterialTheme.typography.body1,
                    )
                    if (!state.done) LinearProgressIndicator(modifier = Modifier.weight(1f))
                }
            }
            items(items = state.apps, key = { it.packageName }) { app ->
                RepoPreviewApp(state.repo ?: error("no repo"), app, localeList)
            }
        }
    }
}

@Composable
fun RepoPreviewHeader(
    state: Fetching,
    onAddRepo: () -> Unit,
    localeList: LocaleListCompat,
) {
    val isDevPreview = LocalInspectionMode.current
    Column(
        verticalArrangement = spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val repo = state.repo ?: error("repo was null")
        Row(
            horizontalArrangement = spacedBy(16.dp),
            verticalAlignment = CenterVertically,
        ) {
            RepoIcon(repo, Modifier.size(48.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = repo.getName(localeList) ?: "Unknown Repository",
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.body1,
                )
                Text(
                    text = repo.address.replaceFirst("https://", ""),
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.alpha(ContentAlpha.medium),
                )
                Text(
                    text = Utils.formatLastUpdated(LocalContext.current.resources, repo.timestamp),
                    style = MaterialTheme.typography.body2,
                )
            }
        }
        if (state.canAdd) FDroidButton(
            text = when (state.fetchResult) {
                is IsNewRepository -> stringResource(R.string.repo_add_new_title)
                is IsNewMirror -> stringResource(R.string.repo_add_new_mirror)
                else -> error("Unexpected fetch state: ${state.fetchResult}")
            },
            onClick = onAddRepo,
            modifier = Modifier.align(End)
        ) else if (state.fetchResult is IsExistingRepository) {
            Text(
                text = stringResource(R.string.repo_exists),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.error,
            )
        }
        val description = if (isDevPreview) {
            LoremIpsum(42).values.joinToString(" ")
        } else {
            repo.getDescription(localeList)
        }
        if (description != null) Text(
            text = description,
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
fun LazyItemScope.RepoPreviewApp(
    repo: Repository,
    app: MinimalApp,
    localeList: LocaleListCompat,
) {
    val isDevPreview = LocalInspectionMode.current
    Card(
        modifier = Modifier
            .animateItemPlacement()
            .fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = spacedBy(8.dp),
            modifier = Modifier.padding(8.dp),
        ) {
            if (isDevPreview) Image(
                painter = rememberDrawablePainter(
                    getDrawable(LocalContext.current.resources, R.drawable.ic_launcher, null)
                ),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
            ) else GlideImage(
                model = getGlideModel(repo, app.getIcon(localeList)),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
            ) {
                it.fallback(R.drawable.ic_repo_app_default).error(R.drawable.ic_repo_app_default)
            }
            Column {
                Text(
                    app.name ?: "Unknown app",
                    style = MaterialTheme.typography.body1,
                )
                Text(
                    app.summary ?: "",
                    style = MaterialTheme.typography.body2,
                )
            }
        }
    }
}

@Preview
@Composable
fun RepoPreviewScreenFetchingPreview() {
    val address = "https://example.org"
    val repo = FDroidApp.createSwapRepo(address, "foo bar")
    val app1 = object : MinimalApp {
        override val repoId = 0L
        override val packageName = "org.example"
        override val name: String = "App 1 with a long name"
        override val summary: String = "Summary of App1 which can also be a bit longer"
        override fun getIcon(localeList: LocaleListCompat): FileV2? = null
    }
    val app2 = object : MinimalApp {
        override val repoId = 0L
        override val packageName = "com.example"
        override val name: String = "App 2 with a name that is even longer than the first app"
        override val summary: String =
            "Summary of App2 which can also be a bit longer, even longer than other apps."

        override fun getIcon(localeList: LocaleListCompat): FileV2? = null
    }
    val app3 = object : MinimalApp {
        override val repoId = 0L
        override val packageName = "net.example"
        override val name: String = "App 3"
        override val summary: String = "short summary"

        override fun getIcon(localeList: LocaleListCompat): FileV2? = null
    }
    FDroidContent {
        RepoPreviewScreen(
            PaddingValues(0.dp),
            Fetching(address, repo, listOf(app1, app2, app3), IsNewRepository)
        ) {}
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 720, heightDp = 360)
fun RepoPreviewScreenNewMirrorPreview() {
    val address = "https://example.org"
    val repo = FDroidApp.createSwapRepo(address, "foo bar")
    FDroidContent {
        RepoPreviewScreen(
            PaddingValues(0.dp),
            Fetching(address, repo, emptyList(), IsNewMirror(0L))
        ) {}
    }
}

@Preview
@Composable
fun RepoPreviewScreenExistingRepoPreview() {
    val address = "https://example.org"
    val repo = FDroidApp.createSwapRepo(address, "foo bar")
    FDroidContent {
        RepoPreviewScreen(
            PaddingValues(0.dp),
            Fetching(address, repo, emptyList(), IsExistingRepository)
        ) {}
    }
}
