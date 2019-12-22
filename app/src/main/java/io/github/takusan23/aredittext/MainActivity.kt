package io.github.takusan23.aredittext

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.rendering.ViewRenderable
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Handler
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var arFragment: ArFragment

    //レイアウトをARに・・・
    lateinit var viewRenderable: ViewRenderable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //条件満たしてなければActivity終了させる
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_main)
        //ArFragment取得
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

        //レイアウトを読み込む
        ViewRenderable.builder()
            .setView(this, R.layout.ar_layout)
            .build()
            .thenAccept { renderable -> viewRenderable = renderable } //読み込み成功
            .exceptionally {
                //読み込み失敗
                it.printStackTrace()
                Toast.makeText(this, "読み込みに失敗しました。", Toast.LENGTH_LONG).show()
                null
            }

        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            if (::viewRenderable.isInitialized) {
                //初期化済みのとき、利用可能
                // Create the Anchor.
                val anchor = hitResult.createAnchor()
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arFragment.arSceneView.scene)

                // Create the transformable andy and add it to the anchor.
                val node = TransformableNode(arFragment.transformationSystem)
                node.setParent(anchorNode)
                node.renderable = viewRenderable
                node.select()
                node.setOnTapListener { hitTestResult, motionEvent ->
                    node.isEnabled = false
                }
            }
        }

        //テキスト変更
        ar_change_button.setOnClickListener {
            //テキスト取得
            val text = ar_change_textview.text.toString()
            //ARで表示するレイアウト取得
            val linearLayout = viewRenderable.view as LinearLayout
            //TextView取得
            val textView = linearLayout.findViewById<TextView>(R.id.textView)
            //変更
            textView.text = text
        }

        //撮影ボタン押したとき
        ar_take_a_picture.setOnClickListener {
            //PixelCopy APIを利用する。のでOreo以降じゃないと利用できません。
            val bitmap = Bitmap.createBitmap(
                arFragment.view?.width ?: 100,
                arFragment.view?.height ?: 100,
                Bitmap.Config.ARGB_8888
            )
            val intArray = IntArray(2)
            arFragment.view?.getLocationInWindow(intArray)

            try {
                PixelCopy.request(
                    arFragment.arSceneView as SurfaceView, //SurfaceViewを継承してるらしい。windowだと真っ暗なので注意！
                    Rect(
                        intArray[0],
                        intArray[1],
                        intArray[0] + (arFragment.view?.width ?: 0),
                        intArray[1] + (arFragment.view?.height ?: 0)
                    ),
                    bitmap,
                    { copyResult: Int ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            //成功時
                            //ここのフォルダは自由に使っていい場所（サンドボックス）
                          val mediaFolder = externalMediaDirs.first()
                          //写真ファイル作成
                          val file = File("${mediaFolder.path}/${System.currentTimeMillis()}.jpg")
                          //Bitmap保存
                          bitmap.compress(Bitmap.CompressFormat.JPEG, 100, file.outputStream())
                          Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
                        }
                    },
                    Handler()
                )
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "失敗しました。", Toast.LENGTH_LONG).show()
            }
        }
    }

    /*
    * Sceneformが利用可能な場合はtrueです。
    * */
    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        val MIN_OPENGL_VERSION = 3.0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(activity, "SceneformにはAndroid N以降が必要です。", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo.glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Toast.makeText(activity, "SceneformにはOpen GL 3.0以降が必要です。", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        return true
    }

}
