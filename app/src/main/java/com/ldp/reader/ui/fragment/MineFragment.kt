package com.ldp.reader.ui.fragment

import android.content.Intent
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ldp.reader.databinding.FragmentMineBinding
import com.ldp.reader.ui.activity.AboutActivity
import com.ldp.reader.ui.activity.LoginActivity
import com.ldp.reader.ui.activity.MainActivity
import com.ldp.reader.ui.activity.ReadingStatsActivity
import com.ldp.reader.ui.activity.SettingsActivity
import com.ldp.reader.ui.base.BaseFragment
import com.ldp.reader.utils.ReadingStatsUtils
import com.ldp.reader.utils.SharedPreUtils

class MineFragment : BaseFragment<FragmentMineBinding>() {
    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMineBinding {
        return FragmentMineBinding.inflate(inflater, container, false)
    }

    override fun initClick() {
        super.initClick()
        binding?.mineLoginEntry?.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        binding?.mineProfileCard?.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        binding?.mineReadingEntry?.setOnClickListener {
            startActivity(Intent(requireContext(), ReadingStatsActivity::class.java))
        }
        binding?.mineBooksEntry?.setOnClickListener {
            (activity as? MainActivity)?.selectHomeTab(MainActivity.HomeTabKey.BOOKSHELF)
        }
        binding?.mineSettingsEntry?.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        binding?.mineAboutEntry?.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
    }

    override fun processLogic() {
        super.processLogic()
        refreshProfile()
    }

    override fun onResume() {
        super.onResume()
        refreshProfile()
    }

    private fun refreshProfile() {
        val userName = SharedPreUtils.getInstance().getString("userName")
        if (!TextUtils.isEmpty(userName)) {
            binding?.mineProfileName?.text = userName
            binding?.mineLoginEntry?.text = "账号"
        } else {
            binding?.mineProfileName?.text = "未登录"
            binding?.mineLoginEntry?.text = "去登录"
        }
        val readingLabel = ReadingStatsUtils.getMineReadingLabel()
        binding?.mineProfileReading?.text = readingLabel
        binding?.mineReadingSummary?.text = readingLabel
    }
}
