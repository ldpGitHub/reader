package com.ldp.reader.cleanup;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class DeprecatedZhuishuCleanupContractTest {

    @Test
    public void manifestDoesNotKeepCommentedLegacyDiscoveryEntries() throws Exception {
        String manifest = new String(
                Files.readAllBytes(new File("src/main/AndroidManifest.xml").toPath()),
                StandardCharsets.UTF_8
        );

        String[] retiredActivities = {
                "BookDiscussionActivity",
                "DiscDetailActivity",
                "BillboardActivity",
                "BookSortActivity",
                "BookSortListActivity",
                "BookListActivity",
                "BookListDetailActivity",
                "BillBookActivity",
                "OtherBillBookActivity",
                "DownloadActivity",
                "CommunityActivity",
                "MoreSettingActivity"
        };

        for (String retiredActivity : retiredActivities) {
            assertFalse(
                    "Manifest should not keep commented legacy entry for " + retiredActivity,
                    manifest.contains(retiredActivity)
            );
        }
    }

    @Test
    public void wholeCommentedLegacySourceStubsAreRemoved() {
        String[] retiredSources = {
                "src/main/java/com/ldp/reader/ui/activity/DownloadActivity.java",
                "src/main/java/com/ldp/reader/presenter/BillBookPresenter.java",
                "src/main/java/com/ldp/reader/presenter/BillboardPresenter.java",
                "src/main/java/com/ldp/reader/presenter/BookListDetailPresenter.java",
                "src/main/java/com/ldp/reader/presenter/BookListPresenter.java",
                "src/main/java/com/ldp/reader/presenter/BookSortListPresenter.java",
                "src/main/java/com/ldp/reader/presenter/BookSortPresenter.java",
                "src/main/java/com/ldp/reader/presenter/CommentDetailPresenter.java",
                "src/main/java/com/ldp/reader/presenter/DiscCommentPresenter.java",
                "src/main/java/com/ldp/reader/presenter/DiscHelpsPresenter.java",
                "src/main/java/com/ldp/reader/presenter/DiscReviewPresenter.java",
                "src/main/java/com/ldp/reader/presenter/HelpsDetailPresenter.java",
                "src/main/java/com/ldp/reader/presenter/ReviewDetailPresenter.java"
        };

        for (String retiredSource : retiredSources) {
            assertFalse(
                    "Remove whole-commented legacy source stub: " + retiredSource,
                    new File(retiredSource).exists()
            );
        }
    }
}
