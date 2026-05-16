package com.ldp.reader.model.bean

import java.io.Serializable

class BookChapterBean() : Serializable {
    var id: String? = null
    @Transient
    var chapterId: Long? = null
    var link: String? = null
    var title: String? = null
    var taskName: String? = null
    private var unreadble: Boolean = false
    private var validInZhuishu: Boolean = true
    var bookId: String? = null
    var start: Long = 0
    var end: Long = 0

    constructor(
        id: String?,
        link: String?,
        title: String?,
        taskName: String?,
        unreadble: Boolean,
        bookId: String?,
        start: Long,
        end: Long
    ) : this() {
        this.id = id
        this.link = link
        this.title = title
        this.taskName = taskName
        this.unreadble = unreadble
        this.bookId = bookId
        this.start = start
        this.end = end
    }

    constructor(
        id: String?,
        link: String?,
        title: String?,
        taskName: String?,
        unreadble: Boolean,
        validInZhuishu: Boolean,
        bookId: String?,
        start: Long,
        end: Long
    ) : this() {
        this.id = id
        this.link = link
        this.title = title
        this.taskName = taskName
        this.unreadble = unreadble
        this.validInZhuishu = validInZhuishu
        this.bookId = bookId
        this.start = start
        this.end = end
    }

    fun isUnreadble(): Boolean = unreadble

    fun setUnreadble(unreadble: Boolean) {
        this.unreadble = unreadble
    }

    fun getUnreadble(): Boolean = unreadble

    fun isValidInZhuishu(): Boolean = validInZhuishu

    fun setValidInZhuishu(validInZhuishu: Boolean) {
        this.validInZhuishu = validInZhuishu
    }

    fun getValidInZhuishu(): Boolean = validInZhuishu

    override fun toString(): String {
        return "BookChapterBean{" +
            "id='" + id + '\'' +
            ", link='" + link + '\'' +
            ", title='" + title + '\'' +
            ", taskName='" + taskName + '\'' +
            ", unreadble=" + unreadble +
            ", bookId='" + bookId + '\'' +
            ", start=" + start +
            ", end=" + end +
            '}'
    }

    companion object {
        private const val serialVersionUID: Long = 56423411313L
    }
}
