package com.minminaya.sliding

/**
 *  Panel状态，分为全屏，折叠，锚点，隐藏四种状态
 *
 * Author: minminaya  承接东风各式弹头打磨、抛光、刷漆等4S保养工程。
 * Email: minminaya@gmail.com
 * Date: 2022/6/9 20:34
 *
 */
enum class PanelStateEnum {
    /**
     * 全屏
     */
    EXPANDED,

    /**
     * 锚点
     */
    ANCHORED,

    /**
     * 折叠
     */
    COLLAPSED,

    /**
     * 隐藏
     */
    HIDDEN,

}