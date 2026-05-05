import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.UserManager.DISALLOW_INSTALL_APPS
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afwsamples.testdpc.DeviceAdminReceiver
import com.afwsamples.testdpc.R
import com.afwsamples.testdpc.common.PackageInstallationUtils
import com.blankj.utilcode.util.AppUtils
import com.bumptech.glide.Glide
import com.goofish.emm.appstore.App
import com.goofish.emm.download.DownloadCallback
import com.goofish.emm.download.DownloadManager
import com.goofish.emm.locktask.KioskConfig
import com.goofish.emm.util.Dpm
import com.tonyodev.fetch2.Download
import java.io.File
import java.io.FileInputStream

class AppGridAdapter(
    private val context: Context,
    private val onDownloadStart: (App) -> Unit,
    private val onDownloadProgress: (Int) -> Unit,
    private val onDownloadComplete: () -> Unit
) : RecyclerView.Adapter<AppGridAdapter.ViewHolder>() {

    private var apps: List<App> = emptyList()

    // 维护下载状态和 ViewHolder 引用
    private val downloadingApps = mutableMapOf<String, DownloadState>()
    private val viewHolders = mutableMapOf<String, ViewHolder>()

    data class DownloadState(
        var progress: Int = 0,
        var isDownloading: Boolean = false,
        var isInstalling: Boolean = false
    )

    // Payload 常量，用于局部更新
    companion object {
        private const val PAYLOAD_PROGRESS = "progress"
        private const val PAYLOAD_STATUS = "status"
    }

    fun setData(newApps: List<App>) {
        apps = newApps
        notifyDataSetChanged()
    }

    private fun getAppPosition(packageName: String): Int {
        return apps.indexOfFirst { it.packageName == packageName }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.app_icon)
        val nameView: TextView = view.findViewById(R.id.app_name)
        val statusText: TextView = view.findViewById(R.id.status_text)
        val statusBadge: ImageView = view.findViewById(R.id.status_badge)
        val progressContainer: View = view.findViewById(R.id.progress_container)
        val downloadProgress: android.widget.ProgressBar = view.findViewById(R.id.download_progress)
        val progressText: TextView = view.findViewById(R.id.progress_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.app_grid_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, mutableListOf())
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        val app = apps[position]

        // 如果是局部更新（有 payload）
        if (payloads.isNotEmpty()) {
            val payload = payloads[0] as? String
            when (payload) {
                PAYLOAD_PROGRESS -> {
                    // 只更新进度条，不重新加载图片等
                    val downloadState = downloadingApps[app.packageName]
                    if (downloadState?.isDownloading == true) {
                        holder.downloadProgress.progress = downloadState.progress
                        holder.progressText.text = "${downloadState.progress}%"
                        Log.d("AppGridAdapter", "Partial update: progress=${downloadState.progress}%")
                    }
                    return
                }
                PAYLOAD_STATUS -> {
                    // 只更新状态，不重新加载图片
                    updateStatusOnly(holder, app)
                    return
                }
            }
        }

        // 完整绑定（首次加载或完全刷新）
        holder.nameView.text = app.name
        Glide.with(holder.itemView.context)
            .load(app.iconUrl)
            .placeholder(android.R.drawable.sym_def_app_icon)
            .error(android.R.drawable.sym_def_app_icon)
            .into(holder.iconView)

        // 保存 ViewHolder 引用
        viewHolders[app.packageName] = holder

        // Check if app is installed
        val isInstalled = AppUtils.isAppInstalled(app.packageName)
        val downloadState = downloadingApps[app.packageName]

        Log.d("AppGridAdapter", "onBindViewHolder (full): pos=$position, pkg=${app.packageName}, " +
                "isInstalled=$isInstalled, downloadState=$downloadState")

        when {
            // 已安装
            isInstalled -> {
                Log.d("AppGridAdapter", "Showing installed state for ${app.name}")
                holder.statusText.text = "已安装"
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_green_dark))
                holder.statusBadge.visibility = View.VISIBLE
                holder.statusBadge.setImageResource(android.R.drawable.checkbox_on_background)
                holder.progressContainer.visibility = View.GONE
                // 清除下载状态和引用
                downloadingApps.remove(app.packageName)
                viewHolders.remove(app.packageName)
            }
            // 安装中
            downloadState?.isInstalling == true -> {
                Log.d("AppGridAdapter", "Showing installing state for ${app.name}")
                holder.statusText.text = "安装中..."
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                holder.statusBadge.visibility = View.GONE
                holder.progressContainer.visibility = View.GONE
            }
            // 下载中
            downloadState?.isDownloading == true -> {
                Log.d("AppGridAdapter", "Showing downloading state for ${app.name}, progress=${downloadState.progress}%")
                holder.statusText.text = "下载中..."
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                holder.statusBadge.visibility = View.GONE
                holder.progressContainer.visibility = View.VISIBLE
                holder.downloadProgress.progress = downloadState.progress
                holder.progressText.text = "${downloadState.progress}%"
                Log.d("AppGridAdapter", "Progress bar set to ${downloadState.progress}%, visibility=${holder.progressContainer.visibility}")
            }
            // 未安装
            else -> {
                Log.d("AppGridAdapter", "Showing not installed state for ${app.name}")
                holder.statusText.text = "点击下载"
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                holder.statusBadge.visibility = View.GONE
                holder.progressContainer.visibility = View.GONE
                viewHolders.remove(app.packageName)
            }
        }

        holder.itemView.setOnClickListener {
            if (isInstalled) {
                launchApp(app.packageName, holder.itemView.context)
            } else if (downloadState?.isDownloading != true) {
                // 只有在未下载时才允许点击下载
                startDownload(app)
            }
        }
    }

    private fun updateStatusOnly(holder: ViewHolder, app: App) {
        val isInstalled = AppUtils.isAppInstalled(app.packageName)
        val downloadState = downloadingApps[app.packageName]

        when {
            isInstalled -> {
                holder.statusText.text = "已安装"
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_green_dark))
                holder.statusBadge.visibility = View.VISIBLE
                holder.statusBadge.setImageResource(android.R.drawable.checkbox_on_background)
                holder.progressContainer.visibility = View.GONE
                downloadingApps.remove(app.packageName)
                viewHolders.remove(app.packageName)
            }
            downloadState?.isInstalling == true -> {
                holder.statusText.text = "安装中..."
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                holder.statusBadge.visibility = View.GONE
                holder.progressContainer.visibility = View.GONE
            }
            downloadState?.isDownloading == true -> {
                holder.statusText.text = "下载中..."
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                holder.statusBadge.visibility = View.GONE
                holder.progressContainer.visibility = View.VISIBLE
                holder.downloadProgress.progress = downloadState.progress
                holder.progressText.text = "${downloadState.progress}%"
            }
            else -> {
                holder.statusText.text = "点击下载"
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                holder.statusBadge.visibility = View.GONE
                holder.progressContainer.visibility = View.GONE
                viewHolders.remove(app.packageName)
            }
        }
    }

    override fun getItemCount() = apps.size

    private fun launchApp(packageName: String, context: Context) {
        Dpm.getInstance().addLockTask(packageName);

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    }

    private fun startDownload(app: App) {
        // 初始化下载状态
        downloadingApps[app.packageName] = DownloadState(
            progress = 0,
            isDownloading = true,
            isInstalling = false
        )

        // 通知全局下载开始
        onDownloadStart(app)

        // 刷新当前项
        val position = getAppPosition(app.packageName)
        if (position >= 0) {
            notifyItemChanged(position)
        }

        val file = File(context.getExternalFilesDir(null), "${app.packageName}.apk")
        if (file.exists()) {
            file.delete()
        }

        DownloadManager.download(app.downloadUrl, file.absolutePath, object : DownloadCallback {
            override fun onStart() {
                Log.i("AppGridAdapter", "Download started for ${app.name}")
            }

            override fun onProgress(progress: Int) {
                Log.d("AppGridAdapter", "onProgress called: ${app.name}, progress=$progress%")

                // 更新全局进度
                onDownloadProgress(progress)

                // 更新下载状态
                val state = downloadingApps[app.packageName]
                if (state != null) {
                    state.progress = progress
                    Log.d("AppGridAdapter", "Updated state for ${app.packageName}: $state")
                } else {
                    Log.e("AppGridAdapter", "State not found for ${app.packageName}!")
                }

                // 使用 Handler 在主线程更新 UI
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    // 直接更新 ViewHolder（避免闪烁）
                    val holder = viewHolders[app.packageName]
                    if (holder != null) {
                        Log.d("AppGridAdapter", "Directly updating ViewHolder progress for ${app.packageName}")
                        holder.downloadProgress.progress = progress
                        holder.progressText.text = "$progress%"
                        holder.progressContainer.visibility = View.VISIBLE
                    } else {
                        // ViewHolder 不存在（可能被回收），使用 payload 局部更新
                        Log.w("AppGridAdapter", "ViewHolder not found, using payload update")
                        val pos = getAppPosition(app.packageName)
                        if (pos >= 0) {
                            notifyItemChanged(pos, PAYLOAD_PROGRESS)
                        }
                    }
                }
            }

            override fun onCompleted(download: Download) {
                Log.i("AppGridAdapter", "Download completed for ${app.name}")

                // 更新为安装中状态
                downloadingApps[app.packageName]?.apply {
                    isDownloading = false
                    isInstalling = true
                }

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    // 使用 payload 局部更新状态
                    val pos = getAppPosition(app.packageName)
                    if (pos >= 0) {
                        notifyItemChanged(pos, PAYLOAD_STATUS)
                    }
                }

                onDownloadComplete()
                installApk(file, app.packageName)
            }
        })/*val fetchConfiguration: FetchConfiguration = FetchConfiguration.Builder(EmmApp.app)
                .setDownloadConcurrentLimit(3)
                .build()

            val fetch = Fetch.Impl.getInstance(fetchConfiguration)

            val request = Request(app.downloadUrl, file.absolutePath)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL

            fetch.enqueue(request, { updatedRequest -> }, { error -> })


            val fetchListener: FetchListener = object : FetchListener {
                override fun onQueued(download: Download, waitingOnNetwork: Boolean) {

                }

                override fun onCompleted(download: Download) {

                    app.isDownloading = false
                    app.isInstalled = true
                    onDownloadComplete()
                    installApk(file)

                }


                override fun onProgress(
                    download: Download,
                    etaInMilliSeconds: Long,
                    downloadedBytesPerSecond: Long
                ) {
                    val progress = download.progress
                    app.downloadProgress = progress
                    onDownloadProgress(progress)
                }

                override fun onPaused(download: Download) {
                }

                override fun onResumed(download: Download) {
                }

                override fun onStarted(
                    download: Download,
                    downloadBlocks: List<DownloadBlock>,
                    totalBlocks: Int
                ) {
                }

                override fun onWaitingNetwork(download: Download) {
                }

                override fun onAdded(download: Download) {
                }

                override fun onCancelled(download: Download) {
                }

                override fun onRemoved(download: Download) {
                }

                override fun onDeleted(download: Download) {
                }

                override fun onDownloadBlockUpdated(
                    download: Download,
                    downloadBlock: DownloadBlock,
                    totalBlocks: Int
                ) {
                }

                override fun onError(
                    download: Download,
                    error: com.tonyodev.fetch2.Error,
                    throwable: Throwable?
                ) {
                }
            }

            fetch.addListener(fetchListener)*/


        /*currentDownloadTask = DownloadTask.Builder(app.downloadUrl, file)
            .setMinIntervalMillisCallbackProcess(16)
            .setPassIfAlreadyCompleted(false)
            .build()

        currentDownloadTask?.enqueue(object : DownloadListener1() {


            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
            }


            override fun taskEnd(
                task: DownloadTask,
                cause: EndCause,
                realCause: java.lang.Exception?,
                model: Listener1Assist.Listener1Model
            ) {

                Log.e("eee", "eee " + cause.name)
                if (cause == EndCause.COMPLETED) {
                    app.isDownloading = false
                    app.isInstalled = true
                    onDownloadComplete()
                    installApk(file)
                } else {
                    // Handle download failure
                    app.isDownloading = false
                    // You might want to notify the user about the failure
                }
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
            }

            override fun fetchEnd(task: DownloadTask, blockIndex: Int, contentLength: Long) {

            }
            override fun connected(
                task: DownloadTask,
                blockCount: Int,
                currentOffset: Long,
                totalLength: Long
            ) {
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                val progress = ((currentOffset.toDouble() / totalLength) * 100).toInt()
                app.downloadProgress = progress
                onDownloadProgress(progress)
            }

        })*/
    }

    private fun installApk(file: File, packageName: String) {
        Log.i("AppGridAdapter", "Installing APK: ${file.absolutePath}")

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = DeviceAdminReceiver.getComponentName(context)
        var installCommitted = false

        try {
            if (KioskConfig.DISALLOW_INSTALL) {
                dpm.clearUserRestriction(admin, DISALLOW_INSTALL_APPS)
                Log.i("AppGridAdapter", "Temporarily cleared DISALLOW_INSTALL_APPS for appstore install")
            }

            FileInputStream(file).use { inputStream ->
                installCommitted = PackageInstallationUtils.installPackage(
                    context,
                    inputStream,
                    packageName,
                    PackageInstallationUtils.INSTALL_SOURCE_APPSTORE
                )
            }
            Log.i("AppGridAdapter", "Appstore install commit sent, package=$packageName, success=$installCommitted")
        } catch (e: Exception) {
            Log.e("AppGridAdapter", "Failed to install APK: $packageName", e)
            downloadingApps.remove(packageName)
            viewHolders.remove(packageName)
            if (KioskConfig.DISALLOW_INSTALL) {
                try {
                    dpm.addUserRestriction(admin, DISALLOW_INSTALL_APPS)
                    Log.i("AppGridAdapter", "Restored DISALLOW_INSTALL_APPS after appstore install failure")
                } catch (restoreError: Exception) {
                    Log.e("AppGridAdapter", "Failed to restore DISALLOW_INSTALL_APPS", restoreError)
                }
            }
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!installCommitted || AppUtils.isAppInstalled(packageName)) {
                downloadingApps.remove(packageName)
                viewHolders.remove(packageName)
            }
            val pos = getAppPosition(packageName)
            if (pos >= 0) {
                notifyItemChanged(pos, PAYLOAD_STATUS)
            }
        }, 2000)
    }
}