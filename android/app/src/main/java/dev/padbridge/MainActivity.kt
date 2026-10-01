package dev.padbridge

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

/**
 * Packet (14 bytes, big-endian, matches Wii pad_pkt):
 *   u32 buttons | s16 lx ly rx ry | u8 lt rt
 * Button bits: 0 A, 1 B, 2 X, 3 Y, 4 LB, 5 RB, 6 Back, 7 Start,
 *              8 L3, 9 R3, 10 Guide, 11 Up, 12 Down, 13 Left, 14 Right
 * Stick Y: up is negative (Android convention).
 */
class MainActivity : Activity() {
    private companion object {
        const val PORT = 7777
        const val SEND_MS = 8L
        val KEY_BITS = mapOf(
            KeyEvent.KEYCODE_BUTTON_A to 0,
            KeyEvent.KEYCODE_BUTTON_B to 1,
            KeyEvent.KEYCODE_BUTTON_X to 2,
            KeyEvent.KEYCODE_BUTTON_Y to 3,
            KeyEvent.KEYCODE_BUTTON_L1 to 4,
            KeyEvent.KEYCODE_BUTTON_R1 to 5,
            KeyEvent.KEYCODE_BUTTON_SELECT to 6,
            KeyEvent.KEYCODE_BUTTON_START to 7,
            KeyEvent.KEYCODE_BUTTON_THUMBL to 8,
            KeyEvent.KEYCODE_BUTTON_THUMBR to 9,
            KeyEvent.KEYCODE_BUTTON_MODE to 10,
            KeyEvent.KEYCODE_DPAD_UP to 11,
            KeyEvent.KEYCODE_DPAD_DOWN to 12,
            KeyEvent.KEYCODE_DPAD_LEFT to 13,
            KeyEvent.KEYCODE_DPAD_RIGHT to 14,
        )
    }

    @Volatile private var keyBits = 0
    @Volatile private var hatBits = 0
    @Volatile private var lx = 0f
    @Volatile private var ly = 0f
    @Volatile private var rx = 0f
    @Volatile private var ry = 0f
    @Volatile private var lt = 0f
    @Volatile private var rt = 0f
    @Volatile private var running = false

    private lateinit var ipBox: EditText
    private lateinit var toggle: Button
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val p = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(p, p * 3, p, p)
        }
        ipBox = EditText(this).apply {
            hint = "Wii IP (shown on Wii screen)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(getPreferences(MODE_PRIVATE).getString("ip", ""))
        }
        toggle = Button(this).apply {
            text = "Start"
            setOnClickListener { if (running) running = false else start() }
        }
        info = TextView(this).apply {
            text = "Connect the Xbox controller to the phone first,\nthen keep this screen open."
            typeface = android.graphics.Typeface.MONOSPACE
        }
        root.addView(ipBox)
        root.addView(toggle)
        root.addView(info)
        setContentView(root)
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun s16(v: Float): Short = (v.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
    private fun u8(v: Float): Byte = (v.coerceIn(0f, 1f) * 255f).toInt().toByte()
    private fun dz(v: Float) = if (abs(v) < 0.08f) 0f else v

    private fun start() {
        val host = ipBox.text.toString().trim()
        if (host.isEmpty()) return
        getPreferences(MODE_PRIVATE).edit().putString("ip", host).apply()
        running = true
        toggle.text = "Stop"
        thread(isDaemon = true) {
            try {
                val addr = InetAddress.getByName(host)
                DatagramSocket().use { sock ->
                    val buf = ByteArray(14)
                    val bb = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN)
                    val pkt = DatagramPacket(buf, buf.size, addr, PORT)
                    var n = 0
                    while (running) {
                        val btn = keyBits or hatBits
                        bb.clear()
                        bb.putInt(btn)
                        bb.putShort(s16(lx)); bb.putShort(s16(ly))
                        bb.putShort(s16(rx)); bb.putShort(s16(ry))
                        bb.put(u8(lt)); bb.put(u8(rt))
                        sock.send(pkt)
                        if (++n % 15 == 0) {
                            val t = String.format(
                                "-> %s:%d\nbtn=%04X\nL %5d %5d\nR %5d %5d\nLT %3d  RT %3d",
                                host, PORT, btn,
                                s16(lx).toInt(), s16(ly).toInt(), s16(rx).toInt(), s16(ry).toInt(),
                                u8(lt).toInt() and 0xFF, u8(rt).toInt() and 0xFF
                            )
                            runOnUiThread { info.text = t }
                        }
                        Thread.sleep(SEND_MS)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { info.text = "error: ${e.message}" }
            } finally {
                running = false
                runOnUiThread { toggle.text = "Start" }
            }
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val bit = KEY_BITS[e.keyCode]
        if (bit != null && (e.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD)) != 0) {
            when (e.action) {
                KeyEvent.ACTION_DOWN -> keyBits = keyBits or (1 shl bit)
                KeyEvent.ACTION_UP -> keyBits = keyBits and (1 shl bit).inv()
            }
            return true // swallow so B doesn't act as Back and D-pad doesn't move UI focus
        }
        return super.dispatchKeyEvent(e)
    }

    override fun dispatchGenericMotionEvent(e: MotionEvent): Boolean {
        if ((e.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            e.action == MotionEvent.ACTION_MOVE
        ) {
            lx = dz(e.getAxisValue(MotionEvent.AXIS_X))
            ly = dz(e.getAxisValue(MotionEvent.AXIS_Y))
            val z = e.getAxisValue(MotionEvent.AXIS_Z)
            val rz = e.getAxisValue(MotionEvent.AXIS_RZ)
            rx = dz(if (z != 0f) z else e.getAxisValue(MotionEvent.AXIS_RX))
            ry = dz(if (rz != 0f) rz else e.getAxisValue(MotionEvent.AXIS_RY))
            lt = max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER), e.getAxisValue(MotionEvent.AXIS_BRAKE))
            rt = max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER), e.getAxisValue(MotionEvent.AXIS_GAS))
            val hx = e.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y)
            hatBits = (if (hy < -0.5f) 1 shl 11 else 0) or
                (if (hy > 0.5f) 1 shl 12 else 0) or
                (if (hx < -0.5f) 1 shl 13 else 0) or
                (if (hx > 0.5f) 1 shl 14 else 0)
            return true
        }
        return super.dispatchGenericMotionEvent(e)
    }
}
