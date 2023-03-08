package xyz.cssxsh.mirai.diffusion

import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.MiraiLogger
import okhttp3.internal.toHexString
import xyz.cssxsh.diffusion.StableDiffusionApiException
import xyz.cssxsh.diffusion.StableDiffusionClient
import xyz.cssxsh.mirai.diffusion.config.ImageToImageConfig
import xyz.cssxsh.mirai.diffusion.config.TextToImageConfig
import java.io.File
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.random.nextUInt

public object StableDiffusionListener : SimpleListenerHost() {

    @PublishedApi
    internal var client: StableDiffusionClient = StableDiffusionClient(config = StableDiffusionConfig)

    @PublishedApi
    internal val configFolder: File by lazy {
        try {
            StableDiffusionHelper.configFolder
        } catch (_: Exception) {
            File("out")
        }
    }

    @PublishedApi
    internal val dataFolder: File by lazy {
        try {
            StableDiffusionHelper.dataFolder
        } catch (_: Exception) {
            File("out")
        }
    }

    @PublishedApi
    internal val json: Json = Json {
        prettyPrint = true
    }

    @PublishedApi
    internal val logger: MiraiLogger = MiraiLogger.Factory.create(this::class.java)

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        val cause = exception.cause as? StableDiffusionApiException ?: exception
        logger.warning(cause)
    }

    @PublishedApi
    internal val reload: Permission by StableDiffusionPermissions

    @EventHandler
    public fun MessageEvent.reload() {
        if (toCommandSender().hasPermission(reload).not()) return
        val content = message.contentToString()
        """(?i)^(?:reload-sd|重载SD)""".toRegex().find(content) ?: return

        with(StableDiffusionHelper) {
            StableDiffusionConfig.reload()
            logger.info("config reloaded")
            client = StableDiffusionClient(config = StableDiffusionConfig)
        }
    }

    @PublishedApi
    internal val txt2img: Permission by StableDiffusionPermissions

    @EventHandler
    public fun MessageEvent.txt2img() {
        if (toCommandSender().hasPermission(txt2img).not()) return

        val content = message.contentToString()

        val match = """(?i)^t2i\s*(\d*)""".toRegex().find(content) ?: return
        val (seed0) = match.destructured
        val next = content.substringAfter('\n', "").ifEmpty { return }
        val seed1 = seed0.toLongOrNull() ?: -1L

        logger.info("t2i for $sender with seed: $seed1")
        val sd = client
        val out = dataFolder

        launch {

            subject.sendMessage(
                buildMessageChain {
                    +At(sender)
                    +"\n 正在努力绘画，请稍等."
                }
            )

            val response = sd.generateTextToImage {

                TextToImageConfig.push(this)

                seed = seed1

                prompt = next.replace("""#(\S+)""".toRegex()) { match ->
                    val (name) = match.destructured
                    styles += name

                    ""
                }.replace("""(\S+)=(\S+)""".toRegex()) { match ->
                    val (key, value) = match.destructured
                    val primitive = when {
                        value.toLongOrNull() != null -> JsonPrimitive(value.toLong())
                        value.toDoubleOrNull() != null -> JsonPrimitive(value.toDouble())
                        value.toBooleanStrictOrNull() != null -> JsonPrimitive(value.toBoolean())
                        else -> JsonPrimitive(value)
                    }
                    raw[key] = primitive

                    ""
                }

                if (styles.isEmpty().not()) logger.info("t2i for $sender with styles: $styles")
                if (raw.isEmpty().not()) logger.info("t2i for $sender with ${JsonObject(raw)}")
            }

            val message = when (TextToImageConfig.detailedOutput) {
                true -> buildForwardMessage {
                    val info = Json.decodeFromString(JsonObject.serializer(), response.info)

                    sender says {
                        response.images.mapIndexed { index, image ->
                            val temp = out.resolve(
                                "${LocalDate.now()}/${seed1}.${
                                    response.hashCode().toHexString()
                                }.${index}.png"
                            )
                            temp.parentFile.mkdirs()
                            temp.writeBytes(image.decodeBase64Bytes())
                            add(subject.uploadImage(temp))
                        }
                    }
                    sender says {
                        appendLine("seed=" + info.get("seed").toString())
                        appendLine("height=" + info.get("height").toString())
                        appendLine("width=" + info.get("width").toString())
                        appendLine("steps=" + info.get("steps").toString())
                        appendLine("cfg_scale=" + info.get("cfg_Scale").toString())
                        appendLine("sampler=" + info.get("sampler_name"))
                        appendLine("batch_size=" + info.get("batch_size").toString())
                        appendLine("styles=" + info.get("styles").toString())
                    }
                    sender says {
                        appendLine("prompt:")
                        appendLine(info.get("prompt").toString())
                    }
                    sender says {
                        appendLine("negative prompt:")
                        appendLine(info.get("negative_prompt").toString())
                    }
                }

                false -> response.images.mapIndexed { index, image ->
                    val temp =
                        out.resolve("${LocalDate.now()}/${seed1}.${response.hashCode().toHexString()}.${index}.png")
                    temp.parentFile.mkdirs()
                    temp.writeBytes(image.decodeBase64Bytes())

                    subject.uploadImage(temp)
                }.toMessageChain()
            }

            subject.sendMessage(message)
            subject.sendMessage(At(sender))
        }
    }

    @PublishedApi
    internal val img2img: Permission by StableDiffusionPermissions

    @EventHandler
    public fun MessageEvent.img2img() {
        if (toCommandSender().hasPermission(img2img).not()) return

        val content = message.findIsInstance<PlainText>()?.content ?: return

        val match = """(?i)^i2i\s*(\d*)""".toRegex().find(content) ?: return
        val (seed0) = match.destructured
        val next = content.substringAfter('\n', "").ifEmpty { return }
        val seed1 = seed0.toLongOrNull() ?: Random.nextUInt().toLong()

        logger.info("i2i for $sender with seed: $seed1")
        val sd = client
        val out = dataFolder

        launch {

            subject.sendMessage(
                buildMessageChain {
                    +At(sender)
                    +"\n 正在执行图生图，请稍等."
                }
            )

            val images = message.filterIsInstance<Image>().associateWith { image ->
                ImageFileHolder.load(image)
            }

            val response = sd.generateImageToImage {

                ImageToImageConfig.push(this)

                seed = seed1

                images {
                    for ((image, file) in images) {
                        val type = when (image.imageType) {
                            ImageType.PNG, ImageType.APNG -> "png"
                            ImageType.BMP -> "bmp"
                            ImageType.JPG -> "jpeg"
                            ImageType.GIF, ImageType.UNKNOWN -> "gif"
                        }
                        add("data:image/${type};base64,${file.readBytes().encodeBase64()}")
                    }
                }

                prompt = next.replace("""#(\S+)""".toRegex()) { match ->
                    val (name) = match.destructured
                    styles += name

                    ""
                }.replace("""(\S+)=(\S+)""".toRegex()) { match ->
                    val (key, value) = match.destructured
                    val primitive = when {
                        value.toLongOrNull() != null -> JsonPrimitive(value.toLong())
                        value.toDoubleOrNull() != null -> JsonPrimitive(value.toDouble())
                        value.toBooleanStrictOrNull() != null -> JsonPrimitive(value.toBoolean())
                        else -> JsonPrimitive(value)
                    }
                    raw[key] = primitive

                    ""
                }
                if (styles.isEmpty().not()) logger.info("t2i for $sender with styles: $styles")
                if (raw.isEmpty().not()) logger.info("t2i for $sender with ${JsonObject(raw)}")
            }

            val message = when (ImageToImageConfig.detailedOutput) {
                true -> buildForwardMessage() {
                    val info = Json.decodeFromString(JsonObject.serializer(), response.info)

                    sender says response.images.mapIndexed { index, image ->
                        val temp =
                            out.resolve("${LocalDate.now()}/${seed1}.${response.hashCode().toHexString()}.${index}.png")
                        temp.parentFile.mkdirs()
                        temp.writeBytes(image.decodeBase64Bytes())

                        subject.uploadImage(temp)
                    }.toMessageChain()

                    sender says {
                        appendLine("seed=" + info.get("seed").toString())
                        appendLine("height=" + info.get("height").toString())
                        appendLine("width=" + info.get("width").toString())
                        appendLine("steps=" + info.get("steps").toString())
                        appendLine("cfg_scale=" + info.get("cfg_Scale").toString())
                        appendLine("sampler=" + info.get("sampler_name"))
                        appendLine("batch_size=" + info.get("batch_size").toString())
                        appendLine("styles=" + info.get("styles").toString())
                    }

                    sender says {
                        appendLine("prompt:")
                        appendLine(info.get("prompt").toString())
                    }

                    sender says {
                        appendLine("negative prompt:")
                        appendLine(info.get("negative_prompt").toString())
                    }

                    sender says buildMessageChain {
                        +"原图:\n"
                        for ((image, file) in images) {
                            +image
                        }
                    }

                }

                false -> response.images.mapIndexed { index, image ->
                    val temp =
                        out.resolve("${LocalDate.now()}/${seed1}.${response.hashCode().toHexString()}.${index}.png")
                    temp.parentFile.mkdirs()
                    temp.writeBytes(image.decodeBase64Bytes())

                    subject.uploadImage(temp)
                }.toMessageChain()
            }

            subject.sendMessage(message)
            subject.sendMessage(At(sender))
        }
    }

    @PublishedApi
    internal val styles: Permission by StableDiffusionPermissions

    @EventHandler
    public fun MessageEvent.styles() {
        if (toCommandSender().hasPermission(styles).not()) return
        val content = message.contentToString()
        """(?i)^(?:styles|风格)""".toRegex().find(content) ?: return

        logger.info("styles for $sender")
        val sd = client

        launch {
            val info = sd.getPromptStyles()
            val message = buildString {
                for (style in info) {
                    appendLine(style.name)
                }
                ifEmpty {
                    appendLine("内容为空")
                }
            }

            subject.sendMessage(message)
        }
    }
}