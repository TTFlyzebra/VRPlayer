package com.flyzebra.vrplayer;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.flyzebra.vrplayer.control.InitConfig;
import com.flyzebra.vrplayer.control.MySyntherizer;
import com.flyzebra.vrplayer.listener.FileSaveListener;
import com.flyzebra.vrplayer.util.FileUtil;

/**
 * 点击合成按钮，保存录音文件
 * <p>
 * Created by fujiayi on 2017/9/15.
 */

public class SaveFileActivity extends SynthActivity {
    {
        descTextId = R.raw.savefile_activity_description;
    }

    /**
     * 与SynthActivity相比，修改listener为FileSaveListener 可实现保存录音功能。
     * 获取的音频内容同speak方法播出的声音
     * FileSaveListener 在UiMessageListener的基础上，使用 onSynthesizeDataArrived回调，获取音频流
     */
    protected void initialTts() {
        String tmpDir = FileUtil.createTmpDir(this);
        // 设置初始化参数
        // 此处可以改为 含有您业务逻辑的SpeechSynthesizerListener的实现类
        SpeechSynthesizerListener listener = new FileSaveListener(mainHandler, tmpDir);


        // appId appKey secretKey 网站上您申请的应用获取。注意使用离线合成功能的话，需要应用中填写您app的包名。包名在build.gradle中获取。
        InitConfig initConfig = getInitConfig(listener);
        synthesizer = new MySyntherizer(this, initConfig, mainHandler); // 此处可以改为MySyntherizer 了解调用过程

        SpeechSynthesizer.getInstance().setAudioAttributes(0,0);
        SpeechSynthesizer.getInstance().setAudioAttributes(1,0);
        SpeechSynthesizer.getInstance().setAudioAttributes(2,0);
        SpeechSynthesizer.getInstance().setAudioAttributes(3,0);
        SpeechSynthesizer.getInstance().setAudioAttributes(4,0);
    }
}
