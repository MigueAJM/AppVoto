package com.example.miivoto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.miivoto.DataBase.adminDB
import com.example.miivoto.RecyclerView.CandidatoAdapter
import com.example.miivoto.RecyclerView.candidato
import com.example.miivoto.Volley.VolleySingleton
import com.example.miivoto.Volley.address
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewAdapter: CandidatoAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    val candidatoList: List<candidato> = ArrayList()
    var id: Int = 0
    var nombre: String = ""
    var carrera: String = ""
    var descrip: String = ""
    var ncontrol: String = ""
    private lateinit var sControl : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val actividad = intent
        if(actividad != null && actividad.hasExtra("ncontrol")){
            sControl = actividad.getStringExtra("ncontrol")
        }else {
            val admin = adminDB(this)
            val result = admin.Consulta("Select ncontrol from usuario")
            if(result!!.moveToFirst()){
                sControl = result.getString(0)
                result.close()
                admin.close()
                getresultadoWs()
            }else{
                val actividadLog = Intent(this, ActivityLogin::class.java)
                startActivity(actividadLog)
                getCandidatosWs()
                getresultadoWs()
            }
        }
        btnAgregarMa.setOnClickListener {
            startActivity(Intent(this,ActivityRegistro::class.java))
            getCandidatosWs()
        }
        //Recycler View start
        viewManager = LinearLayoutManager(this)
        viewAdapter = CandidatoAdapter(
            candidatoList,
            this,
            { candid: candidato -> onItemClickListener(candid) })
        rv_candidatolist.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@MainActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
        }


        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                val position = viewHolder.adapterPosition
                val candid = viewAdapter.getTask()
                val admin = adminDB(baseContext)
                if (admin.Ejecuta(
                        "Delete From candidato Where id_candidato ="
                                + candid[position].id_candidato
                    ) == true
                ) {
                    retrieveCandidato()
                }
            }
        }).attachToRecyclerView(rv_candidatolist)
    }

    private fun onItemClickListener(candid: candidato) {
        val actividad = Intent(this, ActivityDetalle::class.java)
        actividad.putExtra("id_candidato",candid.id_candidato)
        actividad.putExtra("nombre",candid.nombre_alumno)
        actividad.putExtra("carrera",candid.carrera)
        actividad.putExtra("descripcion", candid.descripcion)
        actividad.putExtra("ncontrol",candid.ncontrol)
        startActivity(actividad)
        //descrip = ""
    }

    override fun onResume() {
        super.onResume()
        retrieveCandidato()
    }

    private fun retrieveCandidato() {
        val candidatoX = getCandidatos()
        viewAdapter.setTask(candidatoX!!)

    }

    fun getCandidatos(): MutableList<candidato> {
        var candidato: MutableList<candidato> = ArrayList()
        val admin = adminDB(this)
        val tupla = admin.Consulta("Select * From candidato Order By id_candidato")
        while (tupla!!.moveToNext()) {
            id = tupla.getInt(0)
            nombre = tupla.getString(1)
            carrera = tupla.getString(2)
            descrip = tupla.getString(3)
            ncontrol = tupla.getString(4)
            candidato.add(candidato(id, nombre, carrera, descrip, ncontrol))
        }
        tupla.close()
        admin.close()
        return candidato
    }

    fun getCandidatosWs() { //funcion que carga la informacion de MySQL a SQLite
        val wsURL = address.IP + "Wservice/getCandidatos.php"
        val admin = adminDB(this)
        admin.Ejecuta("DELETE FROM candidato")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, wsURL, null,
            Response.Listener { response ->
                val succ = response["success"]
                val msg = response["message"]
                val candidatoJson = response.getJSONArray("candidato")//name usuario (webservice)
                for (i in 0 until candidatoJson.length()) {
                    val idc = candidatoJson.getJSONObject(i).getString("id_candidato")
                    val nombre = candidatoJson.getJSONObject(i).getString("nombre")
                    val carrera = candidatoJson.getJSONObject(i).getString("carrera")
                    val descripcion = candidatoJson.getJSONObject(i).getString("descripcion")
                    val ncontrol = candidatoJson.getJSONObject(i).getString("ncontrol")
                    val sentencia =
                        "INSERT INTO candidato(id_candidato,nombre,carrera,descripcion,ncontrol) Values('$idc','$nombre','$carrera','$descripcion','$ncontrol')"
                    var result = admin.Ejecuta(sentencia)
                   // Toast.makeText(this, "Información cargada: " + result, Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error capa8: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }
    fun getresultadoWs() { //funcion que carga la informacion de MySQL a SQLite
        val wsURL = address.IP + "Wservice/getresultado.php"
        val admin = adminDB(this)
        admin.Ejecuta("DELETE FROM voto")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, wsURL, null,
            Response.Listener { response ->
                val succ = response["success"]
                val msg = response["message"]
                val resultadoJson = response.getJSONArray("voto")//name usuario (webservice)
                for (i in 0 until resultadoJson.length()) {
                    val id_v = resultadoJson.getJSONObject(i).getString("id_voto")
                    val id_c = resultadoJson.getJSONObject(i).getString("id_candidato")
                    val nombre = resultadoJson.getJSONObject(i).getString("nombre")
                    val sentencia = "INSERT INTO voto(id_voto,id_candidato,nombre) Values($id_v,$id_c,'$nombre')"
                    var result = admin.Ejecuta(sentencia)
                   // Toast.makeText(this, "$result", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error capa8: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btnresultado -> {
                startActivity(Intent(this,Activityresult::class.java))
                return true
            }
            R.id.btnencuesta ->{
                startActivity(Intent(this,ActivityEncuesta::class.java))
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }
}