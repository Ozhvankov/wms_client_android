package com.salor.ventgo.ui.activity.assets_keluar.list

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.support.transition.TransitionManager
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.salor.ventgo.R
import com.salor.ventgo.db.DBS
import com.salor.ventgo.helper.Cons
import com.salor.ventgo.helper.See
import com.salor.ventgo.obj.asset_list_barang_keluar.AssetListBarangKeluar
import com.salor.ventgo.obj.warehouse_list.WarehouseList
import com.salor.ventgo.service.ApiClient
import com.salor.ventgo.ui.activity.BaseActivity
import com.salor.ventgo.ui.activity.assets_keluar.ScannerActivity
import com.salor.ventgo.ui.adapter.assets_keluar.AssetsKeluarListAdapter
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_assets_masuk_list.*
import kotlinx.android.synthetic.main.dialog_failure_custom.*
import kotlinx.android.synthetic.main.item_dialog_tambah_stock_opname.*
import kotlinx.android.synthetic.main.item_empty_data.*
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.io.IOException
import java.util.*

class AssetsKeluarListActivity : BaseActivity() {

    lateinit var timer: Timer
    var str_keyword_search: String = ""
    var isAnim: Boolean = false
    var listWarehouse: ArrayList<WarehouseList> = ArrayList()
    var isNotLoad: Boolean = false
    var listDataBarang: ArrayList<AssetListBarangKeluar> = ArrayList()
    var offset: Int = 0
    var limit: Int = 20
    var str_selected_status: String = ""
    var selected_id_warehouse: Int = 0
    lateinit var spinnerAdapter: AdapterSpinnerStatus
    var listData: ArrayList<String> = ArrayList()
    lateinit var assetsKeluarListAdapter: AssetsKeluarListAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assets_keluar_list)

        setStatusBarGradiantListSearch(this)

        timer = Timer()
        rBack.setOnClickListener(View.OnClickListener { onBackPressed() })


        setData()

        // TODO: 25/07/18 detect nested scroll to bottom
        nestedscrollview.getViewTreeObserver().addOnScrollChangedListener(ViewTreeObserver.OnScrollChangedListener {
            val totalHeight = nestedscrollview.getChildAt(0).getHeight()
            val scrollY = nestedscrollview.getScrollY()
            val isBottomReached = nestedscrollview.canScrollVertically(1)

            if (!isBottomReached) {
                if (!isNotLoad) {
                    pbLoadingBottom.visibility = View.VISIBLE

                    offset += 20

                    getDataListBarang(lBottomLoading, false)

                }
            }

        })


        // TODO search asset keluar barang
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun afterTextChanged(editable: Editable) {

//                rvList.visibility = View.GONE
//                lLoading.visibility = View.VISIBLE
//
//                str_keyword_search = etSearch.getText().toString()
//
//                timer.cancel()
//                timer = Timer()
//                timer.schedule(
//                        object : TimerTask() {
//                            override fun run() {
//
//                                // TODO: do what you need here (refresh list)
//                                try {
//
//                                    if(str_keyword_search == ""){
//                                        isNotLoad = false
//                                    }
//
//                                    this@AssetsKeluarListActivity.runOnUiThread(Runnable {
//                                        try {
//
//                                            listDataBarang.clear()
//                                            offset = 0
//
//                                            getDataListBarang(lLoading,true)
//
//                                        } catch (e: Exception) {
//                                            e.printStackTrace()
//                                        }
//                                    })
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                                // you will probably need to use runOnUiThread(Runnable action) for some specific actions
//                            }
//                        },
//                        5000
//                )
            }
        })

        // TODO set ime click Search
        etSearch.setOnEditorActionListener() { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                str_keyword_search = etSearch.getText().toString()

                isNotLoad = false

                hideKeyboardwithoutPopulate(this@AssetsKeluarListActivity)

                rvList.visibility = View.GONE
                listDataBarang.clear()
                offset = 0

                getDataListBarang(lLoading, true)

            }
            false
        }


        getDataWarehouse()

    }

    private fun getDataWarehouse() {
        pbLoading.visibility = View.VISIBLE
        val service = ApiClient.getClient()

        val idUser = DBS.with(this).idUser.toInt()

        val call = service.warehouseList(idUser)

        See.logE(Cons.CALLRESPONSE, "" + call.request())

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    try {
                        val respon = response.body()!!.string()
                        val json = JSONObject(respon)

                        See.logE("respon_warehouse", respon)

                        val api_status = json.getInt(Cons.API_STATUS)
                        val api_message = json.getString(Cons.API_MESSAGE)

                        if (api_status == Cons.INT_STATUS) {

                            val jsonDataArray = json.getJSONArray(Cons.ITEMS_DATA)

                            val listData = Gson().fromJson(jsonDataArray.toString(), Array<WarehouseList>::class.java).toList()

                            if(listData.isEmpty()){
                                setVisibleEmptyData()
                                pbLoading.visibility = View.GONE
                            }else{
                                listWarehouse.clear()

                                listWarehouse.addAll(listData)

                                selected_id_warehouse = listWarehouse[0].idWarehouse.toInt()

                                spinnerAdapter = AdapterSpinnerStatus(this@AssetsKeluarListActivity, listWarehouse)
                                spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                                spGudang.setAdapter(spinnerAdapter)


                                spGudang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {

                                        isNotLoad = false

                                        selected_id_warehouse = listWarehouse.get(i).idWarehouse.toInt()

                                        listDataBarang.clear()
                                        offset = 0

                                        getDataListBarang(lLoading, false)

                                    }

                                    override fun onNothingSelected(adapterView: AdapterView<*>) {

                                    }
                                }


                                getDataListBarang(lLoading, false)
                            }



                        } else {
                            See.toast(this@AssetsKeluarListActivity, api_message)
                        }

                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                } else {

                    pbLoading.visibility = View.GONE

                    dialogFailure("warehouse",resources.getString(R.string.label_failure_content_server_title),resources.getString(R.string.label_failure_content_server_content))
                 //   See.toast(this@AssetsKeluarListActivity, resources.getString(R.string.label_something_wrong))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                pbLoading.visibility = View.GONE
                dialogFailure("warehouse",resources.getString(R.string.label_failure_content1),resources.getString(R.string.label_failure_content2))
            }
        })

    }


    private fun getDataListBarang(pbLoading: LinearLayout, isSearch: Boolean) {

        pbLoading.visibility = View.VISIBLE
        val service = ApiClient.getClient()

        val call = service.listAssetBarangkeluar(selected_id_warehouse, limit, offset,str_keyword_search)

        See.logE(Cons.CALLRESPONSE, "" + call.request())

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                pbLoading.visibility = View.GONE
                rvList.visibility = View.VISIBLE
                if (response.isSuccessful) {
                    try {
                        val respon = response.body()!!.string()
                        val json = JSONObject(respon)

                        See.logE("respon_asset_barang", respon)

                        val api_status = json.getInt(Cons.API_STATUS)
                        val api_message = json.getString(Cons.API_MESSAGE)

                        if (api_status == Cons.INT_STATUS) {

                            if(offset == 0){
                                listDataBarang.clear()
                            }


                            val jsonDataArray = json.getJSONArray(Cons.ITEMS_DATA)

                            val listData = Gson().fromJson(jsonDataArray.toString(), Array<AssetListBarangKeluar>::class.java).toList()
                            listDataBarang.addAll(listData)

                            assetsKeluarListAdapter.notifyDataSetChanged()

                            if (!isSearch) {
                                if (listData.isEmpty()) {

                                    isNotLoad = true

                                }
                            }



                            if (!isAnim) {

                                setAnimHeader()

                            }

                            if(offset == 0){

                                if (listData.isEmpty()){
                                    setVisibleEmptyData()

                                    return
                                }else{
                                    setVisibleParent()
                                    return
                                }

                            }


                        } else {
                            pbLoading.visibility = View.GONE
                            See.toast(this@AssetsKeluarListActivity, api_message)
                        }

                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                } else {

                    pbLoading.visibility = View.GONE
                    dialogFailure("list",resources.getString(R.string.label_failure_content_server_title),resources.getString(R.string.label_failure_content_server_content))

                    //  See.toast(this@AssetsKeluarListActivity, resources.getString(R.string.label_something_wrong))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                pbLoading.visibility = View.GONE
                dialogFailure("list",resources.getString(R.string.label_failure_content1),resources.getString(R.string.label_failure_content2))
            }
        })

    }

    fun setVisibleParent(){

        setAnimViewVisible(lParentContent,rvList,0)
        setAnimViewGone(lParentContent,lParentEmptyData,0)
      //  lParentEmptyData.visibility = View.GONE

    }

    fun setVisibleEmptyData(){

        rvList.visibility = View.GONE
        setAnimViewVisible(lParentContent,lParentEmptyData,0)

    }

    fun dialogFailure(type: String,title : String,subTitle : String) {
        try {

            var dialog = Dialog(this, R.style.DialogLight)
            dialog.window!!.attributes.windowAnimations = R.style.PauseDialogAnimation
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_failure_custom)
            dialog.setCancelable(false)

            val btnBack: Button = dialog.btnBack
            val btnRefresh: Button = dialog.btnRefresh
            val tv_Content: TextView = dialog.tv_Content
            val tvContent2: TextView = dialog.tvContent2

            tv_Content.text = title
            tvContent2.text = subTitle

            btnBack.setOnClickListener(View.OnClickListener {
                dialog.dismiss()
                onBackPressed()
            })

            btnRefresh.setOnClickListener(View.OnClickListener {
                dialog.dismiss()

                if (type == "list") {
                    getDataListBarang(lLoading, false)
                } else {
                    getDataWarehouse()
                }
            })

            val size = Point()
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            display.getSize(size)
            val mWidth = size.x

            val window = dialog.window
            val wlp = window!!.attributes

            wlp.gravity = Gravity.CENTER
            wlp.x = 0
            wlp.y = 0
            wlp.width = mWidth
            window.attributes = wlp
            dialog.show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setData() {

        assetsKeluarListAdapter = AssetsKeluarListAdapter(this, listDataBarang,this)
        rvList.setAdapter(assetsKeluarListAdapter)
        val layoutManager = LinearLayoutManager(this)
        rvList.setLayoutManager(layoutManager)
        rvList.isNestedScrollingEnabled = false

    }

    fun setAnimHeader() {

        isAnim = true
        try {
            Handler().postDelayed({

                TransitionManager.beginDelayedTransition(lParentContent)
                appSpinner.visibility = View.VISIBLE


            }, 400)
        } catch (e: Exception) {
            e.printStackTrace()
            appSpinner.visibility = View.VISIBLE
        }


    }

    inner class AdapterSpinnerStatus(internal var context: Context, internal var stringList: ArrayList<WarehouseList>) : ArrayAdapter<WarehouseList>(context, R.layout.spinner_item, stringList) {

        internal var inflater: LayoutInflater? = null
        internal var resource: Int = 0
        internal var searchText = ""
        internal var selectedPosisi = 0

        init {
            inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }


        fun setFilter(dataSpinnerExhibitors: List<WarehouseList>, search: String) {
            stringList = java.util.ArrayList()
            stringList.addAll(dataSpinnerExhibitors)
            this.searchText = search
            notifyDataSetChanged()
        }


        fun setSelected(posisi: Int) {
            selectedPosisi = posisi
            notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            var vi = convertView
            val holder: ViewHolder

            if (convertView == null)
                vi = inflater!!.inflate(R.layout.spinner_item, null)
            holder = ViewHolder(vi!!)
            vi.tag = holder

            val item = stringList[position]

            holder.text1.text = item.warehouseName


            if (selectedPosisi == position) {
                //                holder.text1.setTextColor(context.getResources().getColor(R.color.color_text_blue));
                holder.text1.setTextColor(context.resources.getColor(R.color.color_tv_blue_btn_login))
            } else {
                holder.text1.setTextColor(context.resources.getColor(R.color.color_tv_blue_btn_login))
            }


            return vi
        }

        private inner class ViewHolder internal constructor(view: View) {
            internal var text1: TextView

            init {
                text1 = view.findViewById(R.id.text1)

            }
        }
    }

    fun dialogAddStock() {

        try {

            val pDialog = Dialog(this, R.style.DialogLight)
            pDialog!!.window!!.attributes.windowAnimations = R.style.PauseDialogAnimation
            pDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            pDialog!!.setContentView(R.layout.item_dialog_tambah_stock_opname)
            pDialog!!.setCancelable(true)

            val etStock = pDialog.etStock
            val btnTambah = pDialog.btnTambah


            btnTambah.setOnClickListener(View.OnClickListener {

                val str_stock = etStock.text.toString()
                if (str_stock == "") {

                    etStock.requestFocus()
                    etStock.error = resources.getString(R.string.label_form_wajib_diisi)
                } else {

                    pDialog.dismiss()

                }
            })

            val size = Point()
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            display.getSize(size)
            val mWidth = size.x

            val window = pDialog!!.window
            val wlp = window!!.attributes as WindowManager.LayoutParams

            wlp.gravity = Gravity.CENTER
            wlp.x = 0
            wlp.y = 0
            wlp.width = mWidth
            window.attributes = wlp
            pDialog!!.show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        val intent = Intent(this@AssetsKeluarListActivity, ScannerActivity::class.java)
        startActivity(intent)
        finish()
        setOveridePendingTransisi(this@AssetsKeluarListActivity)
    }
}
