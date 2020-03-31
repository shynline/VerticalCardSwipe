package com.shynline.verticalcardswipe


import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class MainActivity : AppCompatActivity() {

    private var firstTimeFlag = false

    private lateinit var verticalCardSwipe: VerticalCardSwipe<ItemModelView, ViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        verticalCardSwipe = findViewById(R.id.oracle)
        val specialAdapter = SpecialAdapter(this)

        verticalCardSwipe.initialize(
                adapter = specialAdapter,
                topLayout = R.layout.otop,
                topLayoutInitializer = {
                    it.findViewById<TextView>(R.id.text).setTextColor(Color.CYAN)
                },
                bottomLayout = R.layout.obottom,
                bottomLayoutInitializer = {

                },
                expireLayout = R.layout.expire,
                expireLayoutInitializer = {

                })


        verticalCardSwipe.verticalCardsActionListener = object : VerticalCardSwipe.VerticalCardsActionListener<ItemModelView> {
            override fun onReleasedToActionBottom(item: ItemModelView?, expired: Boolean) {
                Toast.makeText(this@MainActivity, "release to action from bottom", Toast.LENGTH_SHORT).show()
            }

            override fun onReleasedToActionTop(item: ItemModelView?, expired: Boolean) {
                Toast.makeText(this@MainActivity, "release to action from top", Toast.LENGTH_SHORT).show()
            }
        }
        verticalCardSwipe.verticalCardsSwipeListener = object : VerticalCardSwipe.VerticalCardsSwipeListener<ItemModelView> {
            override fun onSwipedBottom(item: ItemModelView?, expired: Boolean) {
                Toast.makeText(this@MainActivity, "swipe bottom", Toast.LENGTH_SHORT).show()
            }

            override fun onSwipedTop(item: ItemModelView?, expired: Boolean) {
                Toast.makeText(this@MainActivity, "swipe top", Toast.LENGTH_SHORT).show()
            }
        }

        verticalCardSwipe.firstTimeEventListener = object : FirstTimeEventListener<ItemModelView> {
            override fun isSwipingBottomForFirstTime(item: ItemModelView?): Boolean {
                return firstTimeFlag
            }

            override fun isSwipingTopForFirstTime(item: ItemModelView?): Boolean {
                return firstTimeFlag
            }

            override fun swipingTopPaused(item: ItemModelView?, callback: FirstTimeActions) {
                AlertDialog.Builder(this@MainActivity)
                        .setPositiveButton("proceed") { dialog, which -> callback.proceed() }
                        .setNegativeButton("cancel") { dialog, which -> callback.cancel() }
                        .setCancelable(false)
                        .setTitle("swiping top " + item?.id)
                        .show()
            }

            override fun swipingBottomPaused(item: ItemModelView?, callback: FirstTimeActions) {
                AlertDialog.Builder(this@MainActivity)
                        .setPositiveButton("proceed") { dialog, which ->
                            callback.proceed()
                        }
                        .setNegativeButton("cancel") { dialog, which ->
                            callback.cancel()
                        }
                        .setCancelable(false)
                        .setOnDismissListener {
                        }
                        .setTitle("swipe to action top " + item?.id)
                        .show()
            }
        }


        val list = ArrayList<ItemModelView>()
        for (i in 0..1) {
            list.add(ItemModelView(Date().toString()))
        }
        verticalCardSwipe.addCards(list)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        MenuInflater(this)
                .inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val temp: ItemModelView
        when (id) {
            R.id.add_single_card -> {
                verticalCardSwipe.addCard(ItemModelView(Date().toString()))
            }
            R.id.add_multiple_cards -> {
                val list = ArrayList<ItemModelView>()
                for (i in 0..9) {
                    list.add(ItemModelView(Date().toString()))
                }
                verticalCardSwipe.addCards(list)
            }
            R.id.add_single_ad_card -> {
                verticalCardSwipe.addCard(ItemModelView(Date().toString()).apply { ad = true })
            }
            R.id.remove_first_card -> verticalCardSwipe.removeCard(0)
            R.id.remove_second_card -> verticalCardSwipe.removeCard(1)
            R.id.remove_a_random_Card -> {
                if (verticalCardSwipe.currentCardCount() > 0) {
                    verticalCardSwipe.removeCard(Random().nextInt(verticalCardSwipe.currentCardCount()))
                }
            }
            R.id.remove_all_cards -> {
                verticalCardSwipe.cloneCards.forEach {
                    verticalCardSwipe.removeCard(it)
                }
            }
            R.id.update_first_card -> {
                if (verticalCardSwipe.currentCardCount() > 0) {
                    temp = verticalCardSwipe.cloneCards[0]
                    temp.text = "Updated to: ${Date()}"
                    verticalCardSwipe.updateCard(temp, object : VerticalCardSwipe.Updater<ItemModelView> {
                        override fun update(oldItem: ItemModelView, newItem: ItemModelView) {
                            oldItem.text = newItem.text
                        }
                    }, false)
                }
            }
            R.id.update_second_card -> {
                if (verticalCardSwipe.currentCardCount() > 1) {
                    temp = verticalCardSwipe.cloneCards[1]
                    temp.text = "Updated to: ${Date()}"
                    verticalCardSwipe.updateCard(temp, object : VerticalCardSwipe.Updater<ItemModelView> {
                        override fun update(oldItem: ItemModelView, newItem: ItemModelView) {
                            oldItem.text = newItem.text
                        }
                    }, false)
                }
            }
            R.id.update_a_random_card -> {
                if (verticalCardSwipe.currentCardCount() > 0) {
                    temp = verticalCardSwipe.cloneCards[Random().nextInt(verticalCardSwipe.currentCardCount())]
                    temp.text = "Updated to: ${Date()}"
                    verticalCardSwipe.updateCard(temp, object : VerticalCardSwipe.Updater<ItemModelView> {
                        override fun update(oldItem: ItemModelView, newItem: ItemModelView) {
                            oldItem.text = newItem.text
                        }
                    }, false)
                }
            }

            R.id.swipe_top -> {
                verticalCardSwipe.swipeTop(false)
            }
            R.id.swipe_top_silent -> {
                verticalCardSwipe.swipeTop(true)
            }


            R.id.swipe_bottom -> {
                verticalCardSwipe.swipeBottom(false)
            }
            R.id.swipe_bottom_silent -> {
                verticalCardSwipe.swipeBottom(true)
            }

            R.id.toggle_first_time_flag -> {
                firstTimeFlag = firstTimeFlag.not()
                Toast.makeText(this,
                        if (firstTimeFlag) "First time activated" else "First time deactivated",
                        Toast.LENGTH_SHORT).show()
            }
            R.id.toggle_top_action -> {
                topActionFlag = topActionFlag.not()
                verticalCardSwipe.enableTopAction(topActionFlag)
                Toast.makeText(this,
                        if (topActionFlag) "Top action activated" else "Top action deactivated",
                        Toast.LENGTH_SHORT).show()
            }
            R.id.toggle_bottom_action -> {
                bottomActionFlag = bottomActionFlag.not()
                verticalCardSwipe.enableBottomAction(bottomActionFlag)
                Toast.makeText(this,
                        if (firstTimeFlag) "Bottom action activated" else "Bottom action deactivated",
                        Toast.LENGTH_SHORT).show()
            }
            R.id.toggle_ad_prevent_top -> {
                verticalCardSwipe.config.preventSwipeTopIfAd = verticalCardSwipe.config.preventSwipeTopIfAd.not()
                Toast.makeText(this,
                        if (verticalCardSwipe.config.preventSwipeTopIfAd)
                            "Prevent swipe top ad activated" else "Prevent swipe top ad deactivated",
                        Toast.LENGTH_SHORT).show()
            }
            R.id.toggle_ad_prevent_bottom -> {
                verticalCardSwipe.config.preventSwipeBottomIfAd = verticalCardSwipe.config.preventSwipeBottomIfAd.not()
                Toast.makeText(this,
                        if (verticalCardSwipe.config.preventSwipeBottomIfAd)
                            "Prevent swipe bottom ad activated" else "Prevent swipe bottom ad deactivated",
                        Toast.LENGTH_SHORT).show()
            }
            R.id.toggle_expired_prevent_top -> {
                verticalCardSwipe.config.preventSwipeTopIfExpired = verticalCardSwipe.config.preventSwipeTopIfExpired.not()
                Toast.makeText(this,
                        if (verticalCardSwipe.config.preventSwipeTopIfExpired)
                            "Prevent swipe top expired activated" else "Prevent swipe top expired deactivated",
                        Toast.LENGTH_SHORT).show()
            }
            R.id.toggle_expired_prevent_bottom -> {
                verticalCardSwipe.config.preventSwipeBottomIfExpired = verticalCardSwipe.config.preventSwipeBottomIfExpired.not()
                Toast.makeText(this,
                        if (verticalCardSwipe.config.preventSwipeBottomIfExpired)
                            "Prevent swipe bottom expired activated" else "Prevent swipe bottom expired deactivated",
                        Toast.LENGTH_SHORT).show()
            }

        }

        return super.onOptionsItemSelected(item)
    }

    private var topActionFlag = false
    private var bottomActionFlag = false
}
