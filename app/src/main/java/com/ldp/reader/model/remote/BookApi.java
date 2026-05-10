package com.ldp.reader.model.remote;

import com.ldp.reader.model.bean.packages.BookChapterPackage;
import com.ldp.reader.model.bean.packages.ChapterInfoPackage;
import com.ldp.reader.model.bean.packages.HotWordPackage;
import com.ldp.reader.model.bean.packages.KeyWordPackage;
import com.ldp.reader.model.bean.packages.RecommendBookPackage;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by ldp on 17-4-20.
 */

public interface BookApi {


    /**
     * 推荐书籍
     * @param gender
     * @return
     */
    @GET("/book/recommend")
    Single<RecommendBookPackage> getRecommendBookPackage(@Query("gender") String gender);


    /**
     * 获取书籍的章节总列表
     * @param bookId
     * @param view 默认参数为:chapters
     * @return
     */
    @GET("/mix-atoc/{bookId}")
    Single<BookChapterPackage> getBookChapterPackage(@Path("bookId") String bookId, @Query("view") String view);

    /**
     * 章节的内容
     * 这里采用的是同步请求。
     * @param url
     * @return
     */
    @GET("http://chapter2.zhuishushenqi.com/chapter/{url}")
    Single<ChapterInfoPackage> getChapterInfoPackage(@Path("url") String url);

    /************************************搜索书籍******************************************************/
    @GET("/book/hot-word")
    Single<HotWordPackage> getHotWordPackage();

    /**
     * 关键字自动补全
     *
     * @param query
     * @return
     */
    @GET("/book/auto-complete")
    Single<KeyWordPackage> getKeyWordPacakge(@Query("query") String query);

}
