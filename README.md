# [Stable Diffusion Helper](https://github.com/cssxsh/stable-diffusion-helper)

> 基于 [Stable Diffusion web UI](https://github.com/AUTOMATIC1111/stable-diffusion-webui) 的 图片生成插件

[![maven-central](https://img.shields.io/maven-central/v/xyz.cssxsh/stable-diffusion-helper)](https://search.maven.org/artifact/xyz.cssxsh/stable-diffusion-helper)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/1657)

**使用前应该查阅的相关文档或项目**

*   [User Manual](https://github.com/mamoe/mirai/blob/dev/docs/UserManual.md)
*   [Permission Command](https://github.com/mamoe/mirai/blob/dev/mirai-console/docs/BuiltInCommands.md#permissioncommand)
*   [Stable Diffusion web UI Wiki](https://github.com/AUTOMATIC1111/stable-diffusion-webui/wiki/API)

本插件对接的是 `Stable Diffusion web UI` 的 REST API, 需要启动配置中开启选项, 详情请自行查询 [WIKI](https://github.com/AUTOMATIC1111/stable-diffusion-webui/wiki/API)  

## 使用

### `t2i`

以文生图  
权限ID: `xyz.cssxsh.mirai.plugin.stable-diffusion-helper:txt2img`  
例子:
```log
t2i 
steps=50
width=360
height=540
#德克萨斯
night, rain, wet
```
```log
t2i 123456
(8k, RAW photo, best quality, masterpiece:1.2), (realistic, photo-realistic:1.37),omertosa,1girl,(Kpop idol), 
(aegyo sal:1),cute,cityscape, night, rain, wet, professional lighting, photon mapping, radiosity, 
physically-based rendering, <lora:arknightsTexasThe_v10:1>, <lora:koreanDollLikeness_v10:0.5>,Black pantyhose
```

* 设置种子 `t2i $seed`
* 设置参数 `key=value`
* 使用Styles `#xxx`
* 第二行开始才会计入 `prompt`, 所以要两行以上才会触发指令

支持的参数 
* `height` Height `360`
* `width` Width `540`
* `sampler_name` Sampling method `Euler a`
* `steps` Sampling steps `32`
* `batch_size` Batch size `1`
* `n_iter` Batch count `1`
* `cfg_scale` CFG Scale `数字`
* `restore_faces` Restore faces `false`/`true`
* `tiling` Tiling `false`/`true`
* `enable_hr` Hires. fix `false`/`true`
* `hr_second_pass_steps` Hires Steps `0`
* `denoising_strength` Denoising strength `0.7`
* `hr_upscaler` Upscaler `Latent`
* `hr_scale` Upscale by `2.0`

关于 `Negative Prompt`, 由于他和 `Prompt` 一样是分组多词汇的，同时对他们进行支持很麻烦  
所以如果你要用到 `Negative Prompt`, 建议在 `Styles` 加入常用的词组，然后使用 `#xxx` 调用

### `styles`

查看已经载入的 `Styles`  
权限ID: `xyz.cssxsh.mirai.plugin.stable-diffusion-helper:styles`  
例子:
```log
styles 
```
```log
风格 
```

`Styles` 是 `Stable Diffusion web UI` 自带的功能，用于快捷的填充 `prompt` 和 `negative_prompt`  
![Styles.png](.github/Styles.png)

### `reload-sd`

重载`client.yml`  
权限ID: `xyz.cssxsh.mirai.plugin.stable-diffusion-helper:reload`  
例子:
```log
reload-sd
```
```log
重载SD
```

### `i2i`

以图生图  
权限ID: `xyz.cssxsh.mirai.plugin.stable-diffusion-helper:img2img`

支持的参数 基本同 `t2i` 一致

### `samplers`

查看支持的采样器
权限ID: `xyz.cssxsh.mirai.plugin.stable-diffusion-helper:samplers`  
例子:
```log
samplers 
```
```log
采样器 
```

### `models`

查看支持的模型
权限ID: `xyz.cssxsh.mirai.plugin.stable-diffusion-helper:models`  
例子:
```log
models 
```
```log
模型集 
```
```log
model xxxxx
```
```log
模型 xxxxx
```

## 配置

`client.yml` 基本配置

* `base_url` 基本网址
* `dns_over_https` DNS
* `timeout` API超时时间
* `cool_down_time` API冷却时间

`TextToImage.yml` 以图生图基本配置

* `width` 默认宽度
* `height` 默认高度
* `steps` 默认步数
* `sampler` 默认采样器
* `detailed_output` 是否以转发方式输出详细信息 默认 false

`ImageToImage.yml` 以图生图基本配置

* `width` 默认宽度
* `height` 默认高度
* `steps` 默认步数
* `sampler` 默认采样器
* `detailed_output` 是否以转发方式输出详细信息 默认 false

## 安装

### MCL 指令安装

**请确认 mcl.jar 的版本是 2.1.0+**  
`./mcl --update-package xyz.cssxsh:stable-diffusion-helper --channel maven-stable --type plugins`

### 手动安装

1.  从 [Releases](https://github.com/cssxsh/stable-diffusion-helper/releases) 或者 [Maven](https://repo1.maven.org/maven2/xyz/cssxsh/mirai/stable-diffusion-helper/) 下载 `mirai2.jar`
2.  将其放入 `plugins` 文件夹中

## [爱发电](https://afdian.net/@cssxsh)

![afdian](.github/afdian.jpg)