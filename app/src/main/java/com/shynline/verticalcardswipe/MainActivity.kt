package com.shynline.verticalcardswipe


import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*

//import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var flag = false

    lateinit var verticalCardSwipe: VerticalCardSwipe<ItemModelView, ViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        verticalCardSwipe = findViewById(R.id.oracle)
        val specialAdapter = SpecialAdapter(this)

        verticalCardSwipe.config.cardBottomMarginDP = 100f
        verticalCardSwipe.initializeTheOrb(specialAdapter, R.layout.otop, {

        }, R.layout.obottom, {}, R.layout.expire, {})
        verticalCardSwipe.setOracleListener(object : VerticalCardSwipe.OracleListener<ItemModelView> {
            override fun onDragging(percent: Float) {
            }

            override fun onMovedToOrigin(fromDirection: SwipeDirection) {
            }

            override fun onReleasedToAction(item: ItemModelView?) {
                Toast.makeText(this@MainActivity, "swiped to action", Toast.LENGTH_SHORT).show()
            }

            override fun onSwipedTop(item: ItemModelView?) {
                Toast.makeText(this@MainActivity, "discard", Toast.LENGTH_SHORT).show()
            }
        })

        verticalCardSwipe.firstTimeEventListener = object : FirstTimeEventListener<ItemModelView> {
            override fun isSwipingBottomForFirstTime(item: ItemModelView?): Boolean {
                return flag
            }

            override fun isSwipingTopForFirstTime(item: ItemModelView?): Boolean {
                return flag
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
                var intercept = true
                AlertDialog.Builder(this@MainActivity)
                        .setPositiveButton("proceed") { dialog, which ->
                            intercept = false
                            callback.proceed()
                        }
                        .setNegativeButton("cancel") { dialog, which ->
                            intercept = false
                            Log.d("jbasfhabfasd", "negative listener")
                            callback.cancel()
                        }
                        .setCancelable(false)
                        .setOnDismissListener {
                            //                            if (intercept) {
//                                callback.cancel()
//                            }
                        }
                        .setTitle("swipe to action top " + item?.id)
                        .show()
            }
        }


        //        oracle.setOracleListener(new Oracle.OracleListener() {
        //            @Override
        //            public void onDragging(float percent) {
        //                Log.d("percent", String.valueOf(percent));
        //            }
        //
        //            @Override
        //            public void onMovedToOrigin(SwipeDirection fromDirection) {
        //                Toast.makeText(MainActivity.this, "" + (fromDirection == SwipeDirection.Top), Toast.LENGTH_SHORT).show();
        //            }
        //
        //            @Override
        //            public void onReleasedToAction() {
        //                Toast.makeText(MainActivity.this, "action", Toast.LENGTH_SHORT).show();
        //            }
        //
        //            @Override
        //            public void onSwipedTop() {
        //                Toast.makeText(MainActivity.this, "discard", Toast.LENGTH_SHORT).show();
        //            }
        //        });

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        MenuInflater(this)
                .inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val f: ItemModelView
        when (id) {


            R.id.first -> {
//                val yyy = ItemModelView("new single\n" + Date().toString())
//                yyy.ad = true
//                oracle.addCard(yyy)
                flag = !flag
            }
            R.id.second -> {
                val list = ArrayList<ItemModelView>()
                for (i in 0..9) {
                    list.add(ItemModelView("new many\n" + Date().toString()))
                }
                verticalCardSwipe.addCards(list)
            }
            R.id.third -> verticalCardSwipe.removeCard(0)
            R.id.forth -> verticalCardSwipe.removeCard(1)
            R.id.fifth -> {
                val random = Random()

                verticalCardSwipe.removeCard(random.nextInt(verticalCardSwipe.currentCardCount()))
            }
            R.id.sixth -> {
                f = verticalCardSwipe.cloneCards[0]
                f.text = ("update command")
                verticalCardSwipe.updateCard(f, object : VerticalCardSwipe.Updater<ItemModelView> {
                    override fun update(oldItem: ItemModelView, newItem: ItemModelView) {
                        oldItem.text = (newItem.text)
                    }
                }, false)
            }
            R.id.seventh -> {
                f = verticalCardSwipe.cloneCards[1]
                f.text = ("update command")
                verticalCardSwipe.updateCard(f, object : VerticalCardSwipe.Updater<ItemModelView> {
                    override fun update(oldItem: ItemModelView, newItem: ItemModelView) {
                        oldItem.text = (newItem.text)
                    }
                }, false)
            }
            R.id.eighth -> {
                f = verticalCardSwipe.cloneCards[Random().nextInt(verticalCardSwipe.currentCardCount())]
                f.text = ("update command")
                verticalCardSwipe.updateCard(f, object : VerticalCardSwipe.Updater<ItemModelView> {
                    override fun update(oldItem: ItemModelView, newItem: ItemModelView) {
                        oldItem.text = (newItem.text)
                    }
                }, false)
            }
            R.id.remove -> {
                var sss = ArrayList(verticalCardSwipe.cloneCards)
                sss.forEach {
                    verticalCardSwipe.removeCard(it)
                }
            }
            R.id.dismiss -> {
                verticalCardSwipe.swipeTop(false)
            }
            R.id.dismiss_silent -> {
                verticalCardSwipe.swipeTop(true)
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
