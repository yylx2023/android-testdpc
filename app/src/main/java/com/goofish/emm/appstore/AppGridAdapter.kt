import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afwsamples.testdpc.R
import com.blankj.utilcode.util.AppUtils
import com.bumptech.glide.Glide
import com.goofish.emm.appstore.App
import com.goofish.emm.download.DownloadCallback
import com.goofish.emm.download.DownloadManager
import com.goofish.emm.util.Dpm
import com.tonyodev.fetch2.Download
import java.io.File

class AppGridAdapter(
    private val context: Context,
    private val onDownloadStart: (App) -> Unit,
    private val onDownloadProgress: (Int) -> Unit,
    private val onDownloadComplete: () -> Unit
) : RecyclerView.Adapter<AppGridAdapter.ViewHolder>() {

    private var apps: List<App> = emptyList()
//    private var currentDownloadTask: DownloadTask? = null

    fun setData(newApps: List<App>) {
        apps = newApps
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.app_icon)
        val nameView: TextView = view.findViewById(R.id.app_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.app_grid_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]

        holder.nameView.text = app.name
        Glide.with(holder.itemView.context).load(app.iconUrl).into(holder.iconView)

        holder.itemView.setOnClickListener {
            if (AppUtils.isAppInstalled(app.packageName)) {
                launchApp(app.packageName, holder.itemView.context)
            } else {
                startDownload(app)
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
        onDownloadStart(app)

        val file = File(context.getExternalFilesDir(null), "${app.packageName}.apk")
        if (file.exists()) {
            file.delete()
        }
        DownloadManager.download(app.downloadUrl, file.absolutePath, object : DownloadCallback {
            override fun onStart() {

            }

            override fun onProgress(progress: Int) {
                onDownloadProgress(progress)
            }

            override fun onCompleted(download: Download) {

                onDownloadComplete()
                installApk(file)
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

    private fun installApk(file: File) {
        // Implement APK installation logic here
        // Note: This requires additional setup for installing APKs on Android 7.0+
        Log.e("eee", "eeee " + file.absolutePath)

        AppUtils.installApp(file)
    }
}