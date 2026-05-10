package com.ldp.reader.ui;

import static org.junit.Assert.assertNotEquals;

import com.ldp.reader.R;

import org.junit.Test;

public class HomeUiResourceContractTest {

    @Test
    public void homeAndLoginLayoutsKeepBoundResourceIds() {
        assertNotEquals(0, R.layout.activity_main);
        assertNotEquals(0, R.layout.fragment_bookshelf);
        assertNotEquals(0, R.layout.item_coll_book);
        assertNotEquals(0, R.layout.activity_login);
        assertNotEquals(0, R.id.home_shelf_summary);
        assertNotEquals(0, R.id.home_hero_panel);
        assertNotEquals(0, R.id.home_hero_title);
        assertNotEquals(0, R.id.home_hero_subtitle);
        assertNotEquals(0, R.id.home_search_entry);
        assertNotEquals(0, R.id.home_quick_actions);
        assertNotEquals(0, R.id.home_action_search);
        assertNotEquals(0, R.id.home_action_import);
        assertNotEquals(0, R.id.home_action_sync);
        assertNotEquals(0, R.id.home_shelf_title);
        assertNotEquals(0, R.id.home_shelf_subtitle);
        assertNotEquals(0, R.id.home_section_title);
        assertNotEquals(0, R.id.book_shelf_rv_content);
        assertNotEquals(0, R.id.coll_book_iv_cover);
        assertNotEquals(0, R.id.coll_book_tv_name);
        assertNotEquals(0, R.id.coll_book_tv_chapter);
        assertNotEquals(0, R.id.coll_book_tv_lately_update);
        assertNotEquals(0, R.id.iv_login_back);
        assertNotEquals(0, R.id.et_user_phone);
        assertNotEquals(0, R.id.et_sms_code_input);
        assertNotEquals(0, R.id.btn_get_sms_code);
        assertNotEquals(0, R.id.btn_user_login);
        assertNotEquals(0, R.id.btn_direct_login);
        assertNotEquals(0, R.id.btn_user_logout);
        assertNotEquals(0, R.drawable.bg_home_hero_panel);
        assertNotEquals(0, R.drawable.bg_home_search_pill);
        assertNotEquals(0, R.drawable.bg_home_quick_chip);
        assertNotEquals(0, R.drawable.ic_home_search_24);
        assertNotEquals(0, R.drawable.ic_home_import_24);
        assertNotEquals(0, R.drawable.ic_home_sync_24);
    }

    @Test
    public void mainMenuKeepsActionContract() {
        assertNotEquals(0, R.menu.menu_main);
        assertNotEquals(0, R.id.action_search);
        assertNotEquals(0, R.id.action_login);
        assertNotEquals(0, R.id.action_sync_bookshelf);
        assertNotEquals(0, R.id.action_scan_local_book);
    }
}
