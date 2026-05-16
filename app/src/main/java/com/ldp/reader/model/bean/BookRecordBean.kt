package com.ldp.reader.model.bean

class BookRecordBean() {
    var bookId: String? = null
    var chapter: Int = 0
    var pagePos: Int = 0

    constructor(bookId: String?, chapter: Int, pagePos: Int) : this() {
        this.bookId = bookId
        this.chapter = chapter
        this.pagePos = pagePos
    }
}
