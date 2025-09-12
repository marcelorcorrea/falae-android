package org.falaeapp.falae.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.NetworkError
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.falaeapp.falae.BuildConfig
import org.falaeapp.falae.TLSSocketFactory
import org.falaeapp.falae.exception.NoNetworkConnectionException
import org.falaeapp.falae.model.DownloadCache
import org.falaeapp.falae.model.Item
import org.falaeapp.falae.model.User
import org.falaeapp.falae.room.DownloadCacheDao
import org.falaeapp.falae.storage.FileHandler
import org.falaeapp.falae.task.GsonRequest
import org.falaeapp.falae.toFile
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FalaeWebPlatform(val context: Context) {

    private val requestQueue by lazy {
        if (BuildConfig.BASE_URL.startsWith("https")) {
            try {
                // Configuração para HTTPS
                val hurlStack = HurlStack(null, TLSSocketFactory())
                Volley.newRequestQueue(context, hurlStack)
            } catch (e: Exception) {
                Log.e(javaClass.name, "Error creating HTTPS request queue: ${e.message}")
                Volley.newRequestQueue(context)
            }
        } else {
            Volley.newRequestQueue(context)
        }
    }

    suspend fun login(email: String, password: String): User = suspendCoroutine { continuation ->
        try {
            // Verificar conexão com a internet
            if (!hasNetworkConnection()) {
                Log.e(javaClass.name, "No network connection detected during login")
                continuation.resumeWithException(NoNetworkConnectionException("Sem conexão com a internet"))
                return@suspendCoroutine
            }

            // Validar parâmetros
            if (email.isBlank() || password.isBlank()) {
                Log.e(javaClass.name, "Invalid login parameters: email or password is blank")
                continuation.resumeWithException(IllegalArgumentException("Email ou senha inválidos"))
                return@suspendCoroutine
            }

            try {
                // Criar objeto JSON para a requisição
                val credentials = JSONObject()
                credentials.put("email", email)
                credentials.put("password", password)

                val jsonRequest = JSONObject()
                jsonRequest.put("user", credentials)

                val url = BuildConfig.BASE_URL + "/login.json"
                Log.d(javaClass.name, "Connecting to URL: $url")
                Log.d(javaClass.name, "Request body: ${jsonRequest.toString()}")

                // Criar e enviar a requisição
                val request = GsonRequest(
                    url = url,
                    clazz = User::class.java,
                    jsonRequest = jsonRequest,
                    listener = Response.Listener { response ->
                        try {
                            if (response != null) {
                                Log.d(javaClass.name, "Login successful")
                                Log.d(javaClass.name, "Response: $response")
                                continuation.resume(response)
                            } else {
                                Log.e(javaClass.name, "Login response is null")
                                continuation.resumeWithException(NullPointerException("Resposta de login é nula"))
                            }
                        } catch (e: Exception) {
                            Log.e(javaClass.name, "Error processing login response: ${e.message}", e)
                            continuation.resumeWithException(e)
                        }
                    },
                    errorListener = Response.ErrorListener { error ->
                        try {
                            Log.e(javaClass.name, "Login error: ${error.message}")
                            Log.e(javaClass.name, "Error details: ${error.javaClass.simpleName}")

                            // Registrar detalhes específicos do erro
                            when (error) {
                                is ParseError -> {
                                    Log.e(javaClass.name, "Parse error cause: ${error.cause?.message}")
                                }
                                is NetworkError -> {
                                    Log.e(javaClass.name, "Network error")
                                }
                                is ServerError -> {
                                    Log.e(javaClass.name, "Server error: ${error.networkResponse?.statusCode}")
                                }
                                is AuthFailureError -> {
                                    Log.e(javaClass.name, "Auth failure error")
                                }
                                is TimeoutError -> {
                                    Log.e(javaClass.name, "Timeout error")
                                }
                            }

                            continuation.resumeWithException(error)
                        } catch (e: Exception) {
                            Log.e(javaClass.name, "Error in error listener: ${e.message}", e)
                            continuation.resumeWithException(e)
                        }
                    }
                )

                // Adicionar a requisição à fila
                requestQueue.add(request)
            } catch (e: JSONException) {
                Log.e(javaClass.name, "JSON error during login: ${e.message}", e)
                continuation.resumeWithException(e)
            } catch (e: Exception) {
                Log.e(javaClass.name, "Unexpected error during login request: ${e.message}", e)
                continuation.resumeWithException(e)
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "Critical exception during login: ${e.message}", e)
            continuation.resumeWithException(e)
        }
    }

    suspend fun downloadImages(
        user: User,
        downloadCacheDao: DownloadCacheDao,
        fileHandler: FileHandler
    ): User = withContext(Dispatchers.IO) {
        try {
            Log.d(javaClass.name, "Starting downloadImages for user: ${user.name}")

            if (!hasNetworkConnection()) {
                Log.e(javaClass.name, "No network connection detected")
                throw NoNetworkConnectionException("Could not detect any network connection.")
            }

            Log.d(javaClass.name, "Loading cache for user: ${user.email}")
            val userDownloadCache = loadCache(downloadCacheDao, user.email)
            val publicDownloadCache = loadCache(downloadCacheDao, PUBLIC_CACHE_KEY)

            Log.d(javaClass.name, "Creating folders")
            val publicFolder = fileHandler.createPublicFolder(context)
            val userFolder = fileHandler.createUserFolder(context, user.email)

            user.photo?.let { imgSrc ->
                Log.d(javaClass.name, "Processing user photo: $imgSrc")
                launch {
                    if (imgSrc.isNotEmpty()) {
                        try {
                            user.photo = fetchImage(imgSrc, user.name, fileHandler, userFolder, userDownloadCache, user)
                            Log.d(javaClass.name, "User photo processed successfully: ${user.photo}")
                        } catch (e: Exception) {
                            Log.e(javaClass.name, "Error processing user photo: ${e.message}", e)
                        }
                    }
                }
            }

            Log.d(javaClass.name, "Getting all items from spreadsheets")
            val allItems = user.getItemsFromAllSpreadsheets()
            Log.d(javaClass.name, "Total items to process: ${allItems.size}")

            val repeatedItems = getRepeatedItems(allItems)
            Log.d(javaClass.name, "Repeated items: ${repeatedItems.size}")

            coroutineScope {
                allItems.distinctBy { it.imgSrc }
                    .forEach { item: Item ->
                        launch {
                            try {
                                Log.d(javaClass.name, "Processing item: ${item.name}, imgSrc: ${item.imgSrc}")
                                val folder: File
                                val cache: DownloadCache
                                if (item.private) {
                                    folder = userFolder
                                    cache = userDownloadCache
                                    Log.d(javaClass.name, "Item is private, using user folder")
                                } else {
                                    folder = publicFolder
                                    cache = publicDownloadCache
                                    Log.d(javaClass.name, "Item is public, using public folder")
                                }
                                val localUri = fetchImage(item.imgSrc, item.name, fileHandler, folder, cache, user)
                                Log.d(javaClass.name, "Item processed, localUri: $localUri")

                                // Update imgSrc from duplicated items first
                                repeatedItems[item.imgSrc]?.forEach {
                                    it.imgSrc = localUri
                                    Log.d(javaClass.name, "Updated repeated item: ${it.name}")
                                }
                                // Update imgSrc of iterated item
                                item.imgSrc = localUri
                            } catch (e: Exception) {
                                Log.e(javaClass.name, "Error processing item ${item.name}: ${e.message}", e)
                            }
                        }
                    }
            }

            Log.d(javaClass.name, "Saving cache")
            saveOrUpdateCache(downloadCacheDao, userDownloadCache)
            saveOrUpdateCache(downloadCacheDao, publicDownloadCache)

            Log.d(javaClass.name, "Download images completed successfully")
            user
        } catch (e: Exception) {
            Log.e(javaClass.name, "Error in downloadImages: ${e.message}", e)
            throw e
        }
    }

    private fun fetchImage(
        relativeImgPath: String,
        imgName: String,
        fileHandler: FileHandler,
        folder: File,
        cache: DownloadCache,
        user: User
    ): String {
        try {
            Log.d(javaClass.name, "fetchImage - relativeImgPath: $relativeImgPath, imgName: $imgName")

            val imgSrc = "${BuildConfig.BASE_URL}${relativeImgPath}"
            Log.d(javaClass.name, "fetchImage - full imgSrc: $imgSrc")

            Log.d(javaClass.name, "fetchImage - creating image file in folder: ${folder.absolutePath}")
            val file = fileHandler.createImg(folder, imgName, imgSrc)
            Log.d(javaClass.name, "fetchImage - file created: ${file.absolutePath}")

            // Verificar se a imagem já está em cache
            val cachedUri = cache.sources[imgSrc]
            if (cachedUri != null) {
                Log.d(javaClass.name, "fetchImage - image found in cache: $cachedUri")
                return cachedUri
            }

            // Se não estiver em cache, fazer o download
            Log.d(javaClass.name, "fetchImage - image not in cache, downloading...")
            val localUri = download(file, user.authToken, imgName, imgSrc)
            Log.d(javaClass.name, "fetchImage - download completed, localUri: $localUri")

            // Armazenar no cache
            cache.store(imgSrc, localUri)
            Log.d(javaClass.name, "fetchImage - image stored in cache")

            return localUri
        } catch (e: Exception) {
            Log.e(javaClass.name, "Error in fetchImage: ${e.message}", e)
            // Retornar uma string vazia em caso de erro para evitar crash
            return ""
        }
    }

    private fun download(
        imgReference: File,
        token: String,
        name: String,
        imgSrc: String
    ): String {
        try {
            Log.d(javaClass.name, "download - Starting download for: $name, URL: $imgSrc")

            val url = URL(imgSrc)
            Log.d(javaClass.name, "download - URL created")

            try {
                val connection = url.openConnection()
                Log.d(javaClass.name, "download - Connection opened")

                // Configurar timeout
                connection.connectTimeout = TIME_OUT
                connection.readTimeout = TIME_OUT

                // Adicionar token de autorização
                Log.d(javaClass.name, "download - Setting authorization token")
                connection.setRequestProperty("Authorization", "Token $token")

                // Conectar
                Log.d(javaClass.name, "download - Connecting...")
                connection.connect()
                Log.d(javaClass.name, "download - Connected successfully")

                // Ler o stream e salvar no arquivo
                Log.d(javaClass.name, "download - Reading input stream and saving to file: ${imgReference.absolutePath}")
                connection.inputStream.toFile(imgReference.absolutePath)
                Log.d(javaClass.name, "download - File saved successfully")

                // Criar URI do arquivo
                val fileUri = Uri.fromFile(imgReference).toString()
                Log.d(javaClass.name, "download - File URI created: $fileUri")

                return fileUri
            } catch (ex: IOException) {
                Log.e(javaClass.name, "download - IOException: ${ex.message}", ex)
                // Verificar se o arquivo existe e tem tamanho
                if (imgReference.exists() && imgReference.length() > 0) {
                    Log.d(javaClass.name, "download - File exists despite error, returning URI")
                    return Uri.fromFile(imgReference).toString()
                }
                ex.printStackTrace()
            } catch (ex: Exception) {
                Log.e(javaClass.name, "download - Unexpected error: ${ex.message}", ex)
                ex.printStackTrace()
            }

            Log.w(javaClass.name, "download - Failed to download, returning empty string")
            return ""
        } catch (ex: Exception) {
            Log.e(javaClass.name, "download - Critical error: ${ex.message}", ex)
            return ""
        }
    }

    private fun getRepeatedItems(items: List<Item>): Map<String, MutableList<Item>> {
        val itemsMap = mutableMapOf<String, MutableList<Item>>()
        val uniqueItems = HashSet<String>()
        items.forEach { item ->
            if (!uniqueItems.add(item.imgSrc)) {
                val listItem = itemsMap[item.imgSrc] ?: mutableListOf()
                listItem.add(item)
                itemsMap[item.imgSrc] = listItem
            }
        }
        return itemsMap.toMap()
    }

    private fun loadCache(downloadCacheDao: DownloadCacheDao, key: String) =
        downloadCacheDao.findByName(key) ?: DownloadCache(name = key, sources = mutableMapOf())

    private suspend fun saveOrUpdateCache(downloadCacheDao: DownloadCacheDao, cache: DownloadCache) =
        withContext(Dispatchers.IO) {
            Log.d(javaClass.name, "Saving ${cache.sources.size} images in ${cache.name} folder.")
            if (!downloadCacheDao.cacheExists(cache.name)) {
                downloadCacheDao.insert(cache)
            } else {
                downloadCacheDao.update(cache)
            }
        }

    private fun hasNetworkConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Usar getAllNetworks() em vez de allNetworks (propriedade depreciada)
            @Suppress("DEPRECATION")
            cm.getAllNetworks().any { network ->
                val capabilities = cm.getNetworkCapabilities(network)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    companion object {

        private const val TIME_OUT = 6000
        const val PUBLIC_CACHE_KEY = "public"
    }
}
