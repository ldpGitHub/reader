package com.ldp.reader.presenter.contract;

import com.ldp.reader.model.bean.BookDetailBeanInOwn;
import com.ldp.reader.model.bean.CollBookBean;
import com.ldp.reader.ui.base.BaseContract;

/**
 * Created by ldp on 17-5-4.
 */

public interface BookDetailContract {
    interface View extends BaseContract.BaseView{
      void finishRefresh(BookDetailBeanInOwn bean);

        void waitToBookShelf();
        void errorToBookShelf();
        void succeedToBookShelf();
    }

    interface Presenter<T extends BaseContract.BaseView> extends BaseContract.BasePresenter<T>{
        void refreshBookDetail(String bookId);
        //添加到书架上
        void addToBookShelf(CollBookBean collBook);
    }
}
