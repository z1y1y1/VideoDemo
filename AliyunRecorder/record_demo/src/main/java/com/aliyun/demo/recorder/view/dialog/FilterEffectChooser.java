package com.aliyun.demo.recorder.view.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.aliyun.demo.R;
import com.aliyun.demo.recorder.view.effects.filter.AlivcFilterChooseFragment;
import com.aliyun.demo.recorder.view.effects.filter.EffectInfo;
import com.aliyun.demo.recorder.view.effects.filter.interfaces.OnFilterItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zsy_18 data:2019/4/22
 */
public class FilterEffectChooser extends BasePageChooser {
    /**
     * 滤镜fragment
     */
    private AlivcFilterChooseFragment filterChooseFragment;
    /**
     * 滤镜item点击listener
     */
    private OnFilterItemClickListener onFilterItemClickListener;
    private int filterPosition;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //适配有底部导航栏的手机，在full的style下会盖住部分视图的bug
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.QUDemoFullStyle);
    }
    @Override
    public List<Fragment> createPagerFragmentList() {
        List<Fragment> fragments = new ArrayList<>();
        filterChooseFragment = new AlivcFilterChooseFragment();

        initFilter();
        fragments.add(filterChooseFragment);
        return fragments;
    }
    @Override
    public void onStart() {
        super.onStart();
        filterChooseFragment.setFilterPosition(filterPosition);
    }
    private void initFilter() {
        filterChooseFragment.setOnFilterItemClickListener(new OnFilterItemClickListener() {
            @Override
            public void onItemClick(EffectInfo effectInfo, int index) {
                if (onFilterItemClickListener != null) {
                    onFilterItemClickListener.onItemClick(effectInfo, index);
                }
            }
        });
    }

    /**
     * 设置滤镜item点击listener
     * @param listener OnFilterItemClickListener
     */
    public void setOnFilterItemClickListener(OnFilterItemClickListener listener) {
        this.onFilterItemClickListener = listener;
    }
    public void setFilterPosition(int filterPosition) {
        this.filterPosition = filterPosition;
    }


}
