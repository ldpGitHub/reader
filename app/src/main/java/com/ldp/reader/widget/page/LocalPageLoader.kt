package com.ldp.reader.widget.page

import android.util.Log
import com.ldp.reader.model.bean.BookChapterBean
import com.ldp.reader.model.bean.CollBookBean
import com.ldp.reader.model.local.BookRepository
import com.ldp.reader.ui.home.BookshelfLocalProgressStore
import com.ldp.reader.utils.Charset as ReaderCharset
import com.ldp.reader.utils.Constant
import com.ldp.reader.utils.FileUtils
import com.ldp.reader.utils.IOUtils
import com.ldp.reader.utils.LogUtils
import com.ldp.reader.utils.MD5Utils
import com.ldp.reader.utils.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Created by ldp on 17-7-1.
 * 问题:
 * 1. 异常处理没有做好
 */
class LocalPageLoader(pageView: PageView, collBook: CollBookBean) : PageLoader(pageView, collBook) {
    // 章节解析模式
    private var mChapterPattern: Pattern? = null

    // 获取书本的文件
    private lateinit var mBookFile: File

    // 编码类型
    private lateinit var mCharset: ReaderCharset

    private val chapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mChapterJob: Job? = null

    init {
        mStatus = STATUS_PARING
    }

    private fun convertTxtChapter(bookChapters: List<BookChapterBean>): MutableList<TxtChapter> {
        val txtChapters = ArrayList<TxtChapter>(bookChapters.size)
        for (bean in bookChapters) {
            val chapter = TxtChapter()
            chapter.title = bean.title
            chapter.start = bean.start
            chapter.end = bean.end
            txtChapters.add(chapter)
        }
        return txtChapters
    }

    /**
     * 未完成的部分:
     * 1. 序章的添加
     * 2. 章节存在的书本的虚拟分章效果
     */
    @Throws(IOException::class)
    private fun loadChapters() {
        val chapters = ArrayList<TxtChapter>()
        // 获取文件流
        val bookStream = RandomAccessFile(mBookFile, "r")
        // 寻找匹配文章标题的正则表达式，判断是否存在章节名
        val hasChapter = checkChapterType(bookStream)
        // 加载章节
        val buffer = ByteArray(BUFFER_SIZE)
        // 获取到的块起始点，在文件中的位置
        var curOffset = 0L
        // block的个数
        var blockPos = 0

        // 获取文件中的数据到buffer，直到没有数据为止
        while (true) {
            val length = bookStream.read(buffer, 0, buffer.size)
            if (length <= 0) break
            ++blockPos
            // 如果存在Chapter
            if (hasChapter) {
                // 将数据转换成String
                val blockContent = String(buffer, 0, length, javaCharset())
                // 当前Block下使过的String的指针
                var seekPos = 0
                // 进行正则匹配
                val matcher = mChapterPattern!!.matcher(blockContent)
                // 如果存在相应章节
                while (matcher.find()) {
                    // 获取匹配到的字符在字符串中的起始位置
                    val chapterStart = matcher.start()

                    // 如果 seekPos == 0 && nextChapterPos != 0 表示当前block处前面有一段内容
                    // 第一种情况一定是序章 第二种情况可能是上一个章节的内容
                    if (seekPos == 0 && chapterStart != 0) {
                        // 获取当前章节的内容
                        val chapterContent = blockContent.substring(seekPos, chapterStart)
                        // 设置指针偏移
                        seekPos += chapterContent.length

                        // 如果当前对整个文件的偏移位置为0的话，那么就是序章
                        if (curOffset == 0L) {
                            // 创建序章
                            val preChapter = TxtChapter()
                            preChapter.title = "序章"
                            preChapter.start = 0
                            preChapter.end = chapterContent.toByteArray(javaCharset()).size.toLong()

                            // 如果序章大小大于30才添加进去
                            if (preChapter.end - preChapter.start > 30) {
                                chapters.add(preChapter)
                            }

                            // 创建当前章节
                            val curChapter = TxtChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = preChapter.end
                            chapters.add(curChapter)
                        } else {
                            // 获取上一章节
                            val lastChapter = chapters[chapters.size - 1]
                            // 将当前段落添加上一章去
                            lastChapter.end += chapterContent.toByteArray(javaCharset()).size.toLong()

                            // 如果章节内容太小，则移除
                            if (lastChapter.end - lastChapter.start < 30) {
                                chapters.remove(lastChapter)
                            }

                            // 创建当前章节
                            val curChapter = TxtChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = lastChapter.end
                            chapters.add(curChapter)
                        }
                    } else {
                        // 是否存在章节
                        if (chapters.size != 0) {
                            // 获取章节内容
                            val chapterContent = blockContent.substring(seekPos, matcher.start())
                            seekPos += chapterContent.length

                            // 获取上一章节
                            val lastChapter = chapters[chapters.size - 1]
                            lastChapter.end =
                                lastChapter.start + chapterContent.toByteArray(javaCharset()).size

                            // 如果章节内容太小，则移除
                            if (lastChapter.end - lastChapter.start < 30) {
                                chapters.remove(lastChapter)
                            }

                            // 创建当前章节
                            val curChapter = TxtChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = lastChapter.end
                            chapters.add(curChapter)
                        } else {
                            // 如果章节不存在则创建章节
                            val curChapter = TxtChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = 0
                            chapters.add(curChapter)
                        }
                    }
                }
            } else {
                // 进行本地虚拟分章
                // 章节在buffer的偏移量
                var chapterOffset = 0
                // 当前剩余可分配的长度
                var strLength = length
                // 分章的位置
                var chapterPos = 0

                while (strLength > 0) {
                    ++chapterPos
                    // 是否长度超过一章
                    if (strLength > MAX_LENGTH_WITH_NO_CHAPTER) {
                        // 在buffer中一章的终止点
                        var end = length
                        // 寻找换行符作为终止点
                        for (i in chapterOffset + MAX_LENGTH_WITH_NO_CHAPTER until length) {
                            if (buffer[i] == ReaderCharset.BLANK) {
                                end = i
                                break
                            }
                        }
                        val chapter = TxtChapter()
                        chapter.title = "第" + blockPos + "章" + "(" + chapterPos + ")"
                        chapter.start = curOffset + chapterOffset + 1
                        chapter.end = curOffset + end
                        chapters.add(chapter)
                        // 减去已经被分配的长度
                        strLength -= end - chapterOffset
                        // 设置偏移的位置
                        chapterOffset = end
                    } else {
                        val chapter = TxtChapter()
                        chapter.title = "第" + blockPos + "章" + "(" + chapterPos + ")"
                        chapter.start = curOffset + chapterOffset + 1
                        chapter.end = curOffset + length
                        chapters.add(chapter)
                        strLength = 0
                    }
                }
            }

            // block的偏移点
            curOffset += length.toLong()

            if (hasChapter) {
                // 设置上一章的结尾
                val lastChapter = chapters[chapters.size - 1]
                lastChapter.end = curOffset
            }

            // 当添加的block太多的时候，执行GC
            if (blockPos % 15 == 0) {
                System.gc()
                System.runFinalization()
            }
        }

        mChapterList = chapters
        IOUtils.close(bookStream)

        System.gc()
        System.runFinalization()
    }

    /**
     * 从文件中提取一章的内容
     */
    private fun getChapterContent(chapter: TxtChapter): ByteArray {
        var bookStream: RandomAccessFile? = null
        try {
            bookStream = RandomAccessFile(mBookFile, "r")
            bookStream.seek(chapter.start)
            val extent = (chapter.end - chapter.start).toInt()
            val content = ByteArray(extent)
            bookStream.read(content, 0, extent)
            return content
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            IOUtils.close(bookStream)
        }

        return ByteArray(0)
    }

    /**
     * 1. 检查文件中是否存在章节名
     * 2. 判断文件中使用的章节名类型的正则表达式
     *
     * @return 是否存在章节名
     */
    @Throws(IOException::class)
    private fun checkChapterType(bookStream: RandomAccessFile): Boolean {
        // 首先获取128k的数据
        val buffer = ByteArray(BUFFER_SIZE / 4)
        val length = bookStream.read(buffer, 0, buffer.size)
        // 进行章节匹配
        for (str in CHAPTER_PATTERNS) {
            val pattern = Pattern.compile(str, Pattern.MULTILINE)
            val matcher = pattern.matcher(String(buffer, 0, length, javaCharset()))
            // 如果匹配存在，那么就表示当前章节使用这种匹配方式
            if (matcher.find()) {
                mChapterPattern = pattern
                // 重置指针位置
                bookStream.seek(0)
                return true
            }
        }

        // 重置指针位置
        bookStream.seek(0)
        return false
    }

    override fun saveRecord() {
        super.saveRecord()
        // 修改当前COllBook记录
        if (mCollBook != null && isChapterListPrepare) {
            // 表示当前CollBook已经阅读
            mCollBook.setIsUpdate(false)
            mCollBook.lastChapter = mChapterList[mCurChapterPos].getTitle()
            mCollBook.lastRead = StringUtils.dateConvert(
                System.currentTimeMillis(),
                Constant.FORMAT_BOOK_DATE
            )
            val progressTenths = calculateProgressTenths(
                mChapterList.size,
                mCurChapterPos,
                getCurrentPagePosition(),
                getCurrentPageCount()
            )
            BookshelfLocalProgressStore.saveProgressTenths(mCollBook.get_id(), progressTenths)
            // 直接更新
            BookRepository.getInstance().saveCollBook(mCollBook)
        }
    }

    override fun closeBook() {
        super.closeBook()
        mChapterJob?.cancel()
        mChapterJob = null
        chapterScope.cancel()
    }

    override fun refreshChapterList() {
        // 对于文件是否存在，或者为空的判断，不作处理。 ==> 在文件打开前处理过了。
        mBookFile = File(mCollBook.cover!!)
        // 获取文件编码
        mCharset = FileUtils.getCharset(mBookFile.absolutePath)

        val lastModified = StringUtils.dateConvert(mBookFile.lastModified(), Constant.FORMAT_BOOK_DATE)

        // 判断文件是否已经加载过，并具有缓存
        if (
            !mCollBook.isUpdate() && mCollBook.updated != null &&
            mCollBook.updated == lastModified &&
            mCollBook.getBookChapters() != null
        ) {
            mChapterList = convertTxtChapter(mCollBook.getBookChapters()!!)
            mCollBook.chaptersCount = mChapterList.size
            isChapterListPrepare = true

            // 提示目录加载完成
            if (mPageChangeListener != null) {
                mPageChangeListener!!.onCategoryFinish(mChapterList)
            }

            // 加载并显示当前章节
            Log.e(TAG, "+refreshChapterList")

            openChapter()

            return
        }

        mChapterJob = chapterScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    loadChapters()
                }
                mChapterJob = null
                isChapterListPrepare = true

                // 提示目录加载完成
                if (mPageChangeListener != null) {
                    mPageChangeListener!!.onCategoryFinish(mChapterList)
                }

                // 存储章节到数据库
                val bookChapterBeanList = ArrayList<BookChapterBean>()
                for (i in 0 until mChapterList.size) {
                    val chapter = mChapterList[i]
                    val bean = BookChapterBean()
                    bean.id = MD5Utils.strToMd5By16(
                        mBookFile.absolutePath + File.separator + chapter.title
                    )
                    bean.title = chapter.getTitle()
                    bean.start = chapter.getStart()
                    bean.setUnreadble(false)
                    bean.end = chapter.getEnd()
                    bookChapterBeanList.add(bean)
                }
                mCollBook.setBookChapters(bookChapterBeanList)
                mCollBook.chaptersCount = mChapterList.size
                mCollBook.updated = lastModified

                BookRepository.getInstance().saveBookChaptersWithAsync(bookChapterBeanList)
                BookRepository.getInstance().saveCollBook(mCollBook)

                // 加载并显示当前章节
                Log.e(TAG, "+refreshChapterList")
                openChapter()
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                chapterError()
                LogUtils.d(TAG, "file load error:$e")
            }
        }
    }

    override fun getChapterReader(chapter: TxtChapter): BufferedReader {
        // 从文件中获取数据
        val content = getChapterContent(chapter)
        val bais = ByteArrayInputStream(content)
        return BufferedReader(InputStreamReader(bais, mCharset.getName()))
    }

    override fun hasChapterData(chapter: TxtChapter): Boolean {
        return true
    }

    override fun onReadableEndReached() {
        saveRecord()
    }

    private fun javaCharset(): Charset {
        return Charset.forName(mCharset.getName())
    }

    companion object {
        private const val TAG = "LocalPageLoader"

        // 默认从文件中获取数据的长度
        private const val BUFFER_SIZE = 512 * 1024

        // 没有标题的时候，每个章节的最大长度
        private const val MAX_LENGTH_WITH_NO_CHAPTER = 10 * 1024

        // 正则表达式章节匹配模式
        private val CHAPTER_PATTERNS = arrayOf(
            "^(.{0,8})(第)([0-9零一二两三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]{1,10})([章节回集卷])(.{0,30})$",
            "^(\\s{0,4})([\\(【《]?(卷)?)([0-9零一二两三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]{1,10})([\\.:：\\u0020\\f\\t])(.{0,30})$",
            "^(\\s{0,4})([\\(（【《])(.{0,30})([\\)）】》])(\\s{0,2})$",
            "^(\\s{0,4})(正文)(.{0,20})$",
            "^(.{0,4})(Chapter|chapter)(\\s{0,4})([0-9]{1,4})(.{0,30})$"
        )
    }
}
