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
                "src/main/java/com/ldp/reader/presenter/ReviewDetailPresenter.java",
                "src/main/java/com/ldp/reader/presenter/contract/BillBookContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/BillboardContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/BookListDetailContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/BookListContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/BookSortContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/BookSortListContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/CommentDetailContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/DiscCommentContact.java",
                "src/main/java/com/ldp/reader/presenter/contract/DiscHelpsContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/DiscReviewContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/HelpsDetailContract.java",
                "src/main/java/com/ldp/reader/presenter/contract/ReviewDetailContract.java",
                "src/main/java/com/ldp/reader/ui/adapter/BillboardAdapter.java",
                "src/main/java/com/ldp/reader/ui/base/BaseTabActivity.java"
        };

        assertFilesRemoved("Remove whole-commented legacy source stub: ", retiredSources);
    }

    @Test
    public void unreachableLegacyDiscoveryUiLayerIsRemoved() {
        String[] retiredSources = {
                "src/main/java/com/ldp/reader/ui/adapter/BillBookAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/BookListDetailAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/BookSortAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/BookSortListAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/CommentAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/DiscCommentAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/DiscHelpsAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/DiscReviewAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/DownLoadAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/GodCommentAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/HorizonTagAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/SectionAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/TagGroupAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/BillBookHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/BookListInfoHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/BookSortHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/BookSortListHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/CommentHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/DiscCommentHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/DiscHelpsHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/DiscReviewHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/DownloadHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/HorizonTagHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/SectionHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/TagChildHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/TagGroupHolder.java"
        };

        String[] retiredResources = {
                "src/main/res/layout/activity_base_tab.xml",
                "src/main/res/layout/activity_bilboard.xml",
                "src/main/res/layout/activity_book_discussion.xml",
                "src/main/res/layout/activity_book_list.xml",
                "src/main/res/layout/activity_book_sort.xml",
                "src/main/res/layout/activity_book_sort_list.xml",
                "src/main/res/layout/activity_community.xml",
                "src/main/res/layout/activity_discussion_detail.xml",
                "src/main/res/layout/activity_more_setting.xml",
                "src/main/res/layout/activity_refresh_list.xml",
                "src/main/res/layout/fragment_community.xml",
                "src/main/res/layout/fragment_find.xml",
                "src/main/res/layout/fragment_scroll_refresh_list.xml",
                "src/main/res/layout/header_book_list_detail.xml",
                "src/main/res/layout/header_disc_detail.xml",
                "src/main/res/layout/header_disc_review_detail.xml",
                "src/main/res/layout/item_billboard_group.xml",
                "src/main/res/layout/item_billborad_child.xml",
                "src/main/res/layout/item_book_list_info.xml",
                "src/main/res/layout/item_comment.xml",
                "src/main/res/layout/item_disc_comment.xml",
                "src/main/res/layout/item_disc_review.xml",
                "src/main/res/layout/item_download.xml",
                "src/main/res/layout/item_horizon_tag.xml",
                "src/main/res/layout/item_section.xml",
                "src/main/res/layout/item_sort.xml",
                "src/main/res/layout/item_tag_child.xml",
                "src/main/res/layout/item_tag_group.xml",
                "src/main/res/layout/layout_disc_detail.xml",
                "src/main/res/layout/layout_disc_detail_comment.xml"
        };

        assertFilesRemoved("Remove unreachable legacy UI source: ", retiredSources);
        assertFilesRemoved("Remove unreachable legacy UI resource: ", retiredResources);
    }

    @Test
    public void bookDetailDoesNotKeepLegacyZhuishuCommunitySections() throws Exception {
        String[] retiredSources = {
                "src/main/java/com/ldp/reader/ui/adapter/BookListAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/HotCommentAdapter.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/BookListHolder.java",
                "src/main/java/com/ldp/reader/ui/adapter/view/HotCommentHolder.java",
                "src/main/java/com/ldp/reader/model/bean/HotCommentBean.java",
                "src/main/java/com/ldp/reader/model/bean/packages/HotCommentPackage.java",
                "src/main/java/com/ldp/reader/model/bean/packages/RecommendBookListPackage.java"
        };

        String[] retiredResources = {
                "src/main/res/layout/item_book_brief.xml",
                "src/main/res/layout/item_hot_comment.xml"
        };

        String detailLayout = readFile("src/main/res/layout/activity_book_detail.xml");
        String detailContract = readFile("src/main/java/com/ldp/reader/presenter/contract/BookDetailContract.java");
        String bookApi = readFile("src/main/java/com/ldp/reader/model/remote/BookApi.java");
        String remoteRepository = readFile("src/main/java/com/ldp/reader/model/remote/RemoteRepository.java");
        String simplifiedChineseStrings = readFile("src/main/res/values/strings.xml");
        String traditionalChineseStrings = readFile("src/main/res/values-zh-rTW/strings.xml");

        String[] retiredDetailTokens = {
                "book_detail_rv_hot_comment",
                "book_detail_tv_more_comment",
                "book_detail_rv_community",
                "book_detail_tv_community",
                "book_detail_tv_posts_count",
                "book_detail_rv_recommend_book_list",
                "book_list_tv_recommend_book_list",
                "追书人数",
                "读者留存率",
                "日更新字数"
        };

        String[] retiredApiTokens = {
                "finishHotComment",
                "finishRecommendBookList",
                "getHotCommnentPackage",
                "getRecommendBookListPackage",
                "getHotComments",
                "getRecommendBookList",
                "HotCommentBean",
                "RecommendBookListPackage"
        };

        String[] retiredStringTokens = {
                "nb.book_detail.hot_comment",
                "nb.book_detail.community",
                "nb.book_detail.posts_count",
                "nb.book_detail.recommend_book_list",
                "热门书评",
                "推薦書單"
        };

        assertFilesRemoved("Remove detail legacy source: ", retiredSources);
        assertFilesRemoved("Remove detail legacy resource: ", retiredResources);
        assertTextAbsent("Remove detail legacy UI token: ", detailLayout, retiredDetailTokens);
        assertTextAbsent("Remove detail legacy contract token: ", detailContract, retiredApiTokens);
        assertTextAbsent("Remove detail legacy API token: ", bookApi, retiredApiTokens);
        assertTextAbsent("Remove detail legacy repository token: ", remoteRepository, retiredApiTokens);
        assertTextAbsent("Remove detail legacy simplified string token: ", simplifiedChineseStrings, retiredStringTokens);
        assertTextAbsent("Remove detail legacy traditional string token: ", traditionalChineseStrings, retiredStringTokens);
    }

    private void assertFilesRemoved(String messagePrefix, String[] paths) {
        for (String path : paths) {
            assertFalse(messagePrefix + path, new File(path).exists());
        }
    }

    private void assertTextAbsent(String messagePrefix, String text, String[] tokens) {
        for (String token : tokens) {
            assertFalse(messagePrefix + token, text.contains(token));
        }
    }

    private String readFile(String path) throws Exception {
        return new String(
                Files.readAllBytes(new File(path).toPath()),
                StandardCharsets.UTF_8
        );
    }
}
