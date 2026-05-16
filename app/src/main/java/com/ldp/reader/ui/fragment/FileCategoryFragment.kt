package com.ldp.reader.ui.fragment

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ldp.reader.R
import com.ldp.reader.databinding.FragmentFileCategoryBinding
import com.ldp.reader.model.local.BookRepository
import com.ldp.reader.ui.adapter.FileSystemAdapter
import com.ldp.reader.utils.FileStack
import com.ldp.reader.utils.FileUtils
import com.ldp.reader.widget.itemdecoration.DividerItemDecoration
import java.io.File
import java.io.FileFilter
import java.util.Collections
import java.util.Comparator

/**
 * Created by ldp on 17-5-27.
 */
class FileCategoryFragment : BaseFileFragment<FragmentFileCategoryBinding>() {
    private lateinit var mTvPath: TextView
    private lateinit var mTvBackLast: TextView
    private lateinit var mRvContent: RecyclerView
    private lateinit var mFileStack: FileStack

    override fun initWidget(savedInstanceState: Bundle?) {
        super.initWidget(savedInstanceState)
        binding?.let {
            mTvPath = it.fileCategoryTvPath
            mTvBackLast = it.fileCategoryTvBackLast
            mRvContent = it.fileCategoryRvContent

            mFileStack = FileStack()
            setUpAdapter()
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFileCategoryBinding {
        return FragmentFileCategoryBinding.inflate(inflater, container, false)
    }

    private fun setUpAdapter() {
        mAdapter = FileSystemAdapter()
        mRvContent.layoutManager = LinearLayoutManager(context)
        mRvContent.addItemDecoration(DividerItemDecoration(context))
        mRvContent.adapter = mAdapter
    }

    override fun initClick() {
        super.initClick()
        mAdapter!!.setOnItemClickListener { _, pos ->
            val file = mAdapter!!.getItem(pos)
            if (file.isDirectory) {
                //保存当前信息。
                val snapshot = FileStack.FileSnapshot()
                snapshot.filePath = mTvPath.text.toString()
                snapshot.files = ArrayList(mAdapter!!.items)
                snapshot.scrollOffset = mRvContent.computeVerticalScrollOffset()
                mFileStack.push(snapshot)
                //切换下一个文件
                toggleFileTree(file)
            } else {
                //如果是已加载的文件，则点击事件无效。
                val id = mAdapter!!.getItem(pos).absolutePath
                if (BookRepository.getInstance().getCollBook(id) != null) {
                    return@setOnItemClickListener
                }
                //点击选中
                mAdapter!!.setCheckedItem(pos)
                //反馈
                mListener?.onItemCheckedChange(mAdapter!!.getItemIsChecked(pos))
            }
        }

        mTvBackLast.setOnClickListener {
            val snapshot = mFileStack.pop()
            val oldScrollOffset = mRvContent.computeHorizontalScrollOffset()
            if (snapshot == null) return@setOnClickListener
            mTvPath.text = snapshot.filePath
            mAdapter!!.refreshItems(snapshot.files!!)
            mRvContent.scrollBy(0, snapshot.scrollOffset - oldScrollOffset)
            //反馈
            mListener?.onCategoryChanged()
        }
    }

    override fun processLogic() {
        super.processLogic()
        val root = Environment.getExternalStorageDirectory()
        toggleFileTree(root)
    }

    private fun toggleFileTree(file: File) {
        //路径名
        mTvPath.text = getString(R.string.nb_file_path, file.path)
        //获取数据
        val files = file.listFiles(SimpleFileFilter())!!
        //转换成List
        val rootFiles = files.toMutableList()
        //排序
        Collections.sort(rootFiles, FileComparator())
        //加入
        mAdapter!!.refreshItems(rootFiles)
        //反馈
        mListener?.onCategoryChanged()
    }

    override val fileCount: Int
        get() {
            var count = 0
            val entrys = mAdapter!!.getCheckMap().entries
            for (entry in entrys) {
                if (!entry.key.isDirectory) {
                    ++count
                }
            }
            return count
        }

    inner class FileComparator : Comparator<File> {
        override fun compare(o1: File, o2: File): Int {
            if (o1.isDirectory && o2.isFile) {
                return -1
            }
            if (o2.isDirectory && o1.isFile) {
                return 1
            }
            return o1.name.compareTo(o2.name, ignoreCase = true)
        }
    }

    inner class SimpleFileFilter : FileFilter {
        override fun accept(pathname: File): Boolean {
            if (pathname.name.startsWith(".")) {
                return false
            }
            //文件夹内部数量为0
            if (pathname.isDirectory && pathname.list()!!.isEmpty()) {
                return false
            }

            /**
             * 现在只支持TXT文件的显示
             */
            //文件内容为空,或者不以txt为开头
            if (!pathname.isDirectory &&
                (pathname.length() == 0L || !pathname.name.endsWith(FileUtils.SUFFIX_TXT))
            ) {
                return false
            }
            return true
        }
    }

    companion object {
        private const val TAG = "FileCategoryFragment"
    }
}
