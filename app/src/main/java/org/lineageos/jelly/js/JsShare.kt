/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.js

import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.lineageos.jelly.models.WebShare
import org.lineageos.jelly.webview.WebViewExtActivity
import java.io.File
import java.io.FileOutputStream

@Keep
class JsShare(
    private val activity: WebViewExtActivity,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cacheDir = File(activity.cacheDir, "share").apply { mkdirs() }

    @JavascriptInterface
    fun share(url: String?, text: String?, title: String?, filesJsonString: String?) {
        cacheFiles(filesJsonString) { files ->
            val webShare = WebShare(url, text, title, files)
            activity.onWebShare(webShare)
        }
    }

    private fun cacheFiles(filesJsonString: String?, callback: (files: List<Uri>) -> Unit) {
        if (filesJsonString == null) {
            callback(emptyList())
            return
        }
        scope.launch {
            val filesJsonArray = JSONArray(filesJsonString)
            val files = buildList {
                for (i in 0 until filesJsonArray.length()) {
                    when (val file = filesJsonArray.get(i)) {
                        is JSONObject -> {
                            val name = file.getString("name")
                            val cacheFile = File(cacheDir, name)

                            val base64 = file.getString("base64")
                            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)

                            FileOutputStream(cacheFile).use {
                                it.write(decodedBytes)
                            }

                            add(
                                FileProvider.getUriForFile(
                                    activity,
                                    "${activity.packageName}.fileprovider",
                                    cacheFile,
                                )
                            )
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                callback(files)
            }
        }
    }

    companion object {
        const val INTERFACE = "JsShare"

        private const val MONKEY_PATCH_ONCE_KEY = "JsShareMonkeyPatch"
        const val SCRIPT = """
            /**
             * @see https://developer.mozilla.org/en-US/docs/Web/API/Web_Share_API
             */
            (() => {
                if (
                    window.$MONKEY_PATCH_ONCE_KEY ||
                    navigator.canShare ||
                    navigator.share
                ) return;

                window.$MONKEY_PATCH_ONCE_KEY = true;

                const dataTypes = new Map();
                dataTypes.set('url', 'string');
                dataTypes.set('text', 'string');
                dataTypes.set('title', 'string');
                dataTypes.set('files', 'object');

                const dataProps = Array.from(dataTypes.keys());

                const fileToBase64 = (file) => new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = () => resolve(reader.result.split(',')[1]);
                    reader.onerror = (error) => reject(error);
                    reader.readAsDataURL(file);
                });

                const share = (data, files = []) => {
                    $INTERFACE.share(
                        data.url ?? null,
                        data.text ?? null,
                        data.title ?? null,
                        files.length ? JSON.stringify(files) : null
                    );
                };

                /**
                 * @see https://developer.mozilla.org/en-US/docs/Web/API/Navigator/canShare
                 */
                navigator.canShare = (data) => true;

                /**
                 * @see https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share
                 */
                navigator.share = (data) => new Promise((resolve, reject) => {
                    // Validating data value
                    if (!data) {
                        reject(new TypeError('The data parameter was omitted completely'));
                        return;
                    }

                    // Validating data type
                    if (typeof data !== 'object' || Array.isArray(data)) {
                        reject(new TypeError('The data type is not object'));
                        return;
                    }

                    // Validating data props
                    if (!dataProps.some(prop => !!data[prop])) {
                        reject(new TypeError(`The data doesn't contain any of its properties`));
                        return;
                    }

                    // Validating data props type
                    for (const prop of dataProps) {
                        const type = dataTypes.get(prop);
                        const value = data[prop];
                        if (!!value && typeof value !== type) {
                            reject(new TypeError('The ' + prop + ' type is not ' + type));
                            return;
                        }
                    }

                    // Validating data files prop
                    const base64Files = [];
                    const files = data['files'];
                    if (!!files) {
                        // files type should be array
                        if (!Array.isArray(files) && !(files instanceof FileList)) {
                            reject(new TypeError('The files type is not array'));
                            return;
                        }
                        // files should contain instances of the File class only
                        for (const file of files) {
                            if (!(file instanceof File)) {
                                reject(new TypeError('The provided file type is not File'));
                                return;
                            }
                        }
                        // convert files to base64
                        if (files.length) {
                            (async () => {
                                for (const file of files) {
                                    try {
                                        const name = file.name;
                                        const base64 = await fileToBase64(file);
                                        base64Files.push({name, base64});
                                    } catch (error) {
                                        reject(error);
                                        return;
                                    }
                                }
                                share(data, base64Files);
                                resolve(undefined);
                            })();
                            return;
                        }
                    }

                    share(data);
                    resolve(undefined);
                });
            })();
        """
    }
}
