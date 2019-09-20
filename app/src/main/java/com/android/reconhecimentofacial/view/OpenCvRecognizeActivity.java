package com.android.reconhecimentofacial.view;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.reconhecimentofacial.controller.CvCameraPreview;
import com.android.reconhecimentofacial.model.Treinamento;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_face;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Size;
import com.android.reconhecimentofacial.R;

import java.io.File;

import static org.bytedeco.javacpp.opencv_core.LINE_8;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_face.createEigenFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_VECTOR0;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import static com.android.reconhecimentofacial.model.Treinamento.NIVEL_DE_ACEITACAO;

/**

 * Criado por Jonathan Brito.

 */

public class OpenCvRecognizeActivity extends Activity implements CvCameraPreview.CvCameraViewListener {

    public static final String TAG = "OpenCvRecognizeActivity";
    private CascadeClassifier faceDetector;
    private String[] nomes = {"", "Usuario Reconhecido"};
    private int absoluteFaceSize = 0;
	private CvCameraPreview cameraView;
    boolean takePhoto;
    opencv_face.FaceRecognizer faceRecognizer = createEigenFaceRecognizer();
    boolean treinared;
    int remainigPhotos = 1;

	//Inicializa as permissões para evitar que o App inicialize sem as Permissões, não consiga acesso aos componentes e feche
    private boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // Inicializa  a Câmera e as funções dos botões
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opencv);


		//Verifica as Permissiões necessárias para armazenamento
        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1 );
            }
        }
		SetarCamera();
        new AsyncTask<Void,Void,Void>() {
			//Realiza as operações de treinamento
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    faceDetector = Treinamento.loadClassifierCascade(OpenCvRecognizeActivity.this, R.raw.frontalface);
                    if(Treinamento.istreinared(getBaseContext())) {
                        File folder = new File(getFilesDir(), Treinamento.treinar_FOLDER);
                        File f = new File(folder, Treinamento.CLASSIFICADOR_EIGEN_FACES);
                        faceRecognizer.load(f.getAbsolutePath());
                        treinared = true;
                    }
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
				//botão que tira foto
                super.onPostExecute(aVoid);
                findViewById(R.id.btPhoto).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        takePhoto = true;
                        alertaQtdFotos();




                    }
                });
				
				/*Caso Precise de um Botão para o treinamento
				//botão que realiza o treinamento o app
                findViewById(R.id.bttreinar).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        treinar();
                    }
                });
				*/
				
				/*Caso precise Resetar o app
				//reset
                findViewById(R.id.btReset).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            Treinamento.reset(getBaseContext());
                            Toast.makeText(getBaseContext(), "Reseted with sucess.", Toast.LENGTH_SHORT).show();
                            finish();
                        }catch (Exception e) {
                            Log.d(TAG, e.getLocalizedMessage(), e);
                        }
                    }
                });
				*/
            }
        }.execute();
    }

	//Chama a Classe Treinamento  
    void treinar() {
        int remainigPhotos = Treinamento.FOTOS_TREINAR_QTD - Treinamento.qtdFotos(getBaseContext());
        Toast.makeText(getBaseContext(), "Iniciado Treinamento ", Toast.LENGTH_LONG).show();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try{
                    if(!Treinamento.istreinared(getBaseContext())) {
                        Treinamento.treinar(getBaseContext());
                    }
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
				      Toast.makeText(getBaseContext(), "Treinamento Realizado com Sucesso, INICIE O RECONHECIMENTO ", Toast.LENGTH_SHORT).show();
                    finish();
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
            }
        }.execute();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        absoluteFaceSize = (int) (width * 0.32f);
    }

    @Override
    public void onCameraViewStopped() {
    }

	//Realiza a Captura e armazenamento das Faces
    private void capturePhoto(Mat rgbaMat) {
        try {
            Treinamento.tirarFoto(getBaseContext(), 1, Treinamento.qtdFotos(getBaseContext()) + 1, rgbaMat.clone(), faceDetector);
        }catch (Exception e) {
            e.printStackTrace();
        }
        takePhoto = false;
    }

   //Realiza o reconhecimento Facial
    private void recognize(opencv_core.Rect dadosFace, Mat grayMat, Mat rgbaMat) {
        Mat detectedFace = new Mat(grayMat, dadosFace);
        resize(detectedFace, detectedFace, new Size(Treinamento.IMG_SIZE,Treinamento.IMG_SIZE));
        IntPointer label = new IntPointer(1);
        DoublePointer reliability = new DoublePointer(1);
        faceRecognizer.predict(detectedFace, label, reliability);
        int prediction = label.get(0);
        double acceptanceLevel = reliability.get(0);
        String name;
        if (prediction == -1 || acceptanceLevel >= NIVEL_DE_ACEITACAO) {
            name = getString(R.string.unknown);
        } else {
            name = nomes[prediction] + " - " + acceptanceLevel;
        }
        int x = Math.max(dadosFace.tl().x() - 10, 0);
        int y = Math.max(dadosFace.tl().y() - 10, 0);
        putText(rgbaMat, name, new Point(x, y), CV_FONT_VECTOR0, 1.0, new opencv_core.Scalar(255,0,0,0));
    }

   //Desenha um retângulo ao redor da face
    void showDetectedFace(RectVector faces, Mat rgbaMat) {
        int x = faces.get(0).x();
        int y = faces.get(0).y();
        int w = faces.get(0).width();
        int h = faces.get(0).height();
        rectangle(rgbaMat, new Point(x, y), new Point(x + w, y + h), opencv_core.Scalar.WHITE, 1, LINE_8, 0);
    }

	//Metodo Principal da Classe que ao pegar um Frame gera uma Matriz
	//e captura e Redimensiona para posteriormente fazer a análise
    @Override
    public Mat onCameraFrame(Mat rgbaMat) {
		// verifica se o classificador detectou alguma face
        if (faceDetector != null) {
            Mat greyMat = new Mat(rgbaMat.rows(), rgbaMat.cols());
            cvtColor(rgbaMat, greyMat, CV_BGR2GRAY);
            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(greyMat, faces, 1.25f, 3, 1,
                    new Size(absoluteFaceSize, absoluteFaceSize),
                    new Size(4 * absoluteFaceSize, 4 * absoluteFaceSize));
        //Quando o Detector verifica uma Face gera um quadrado demarcando a face
		// e Realiza a Caputra da face
            if (faces.size() == 1) {
                showDetectedFace(faces, rgbaMat);
                if(takePhoto) {
                    capturePhoto(rgbaMat);

                }
				//se o Algoritmo ja estiver treinado chama o reconhecimento
                if(treinared) {
                    recognize(faces.get(0), greyMat, rgbaMat);
                }

            }
            greyMat.release();
        }

        return rgbaMat;

    }
	
	void SetarCamera(){
		    cameraView = (CvCameraPreview) findViewById(R.id.camera_view);
            cameraView.setCvCameraViewListener(this);
	}

	//Alerta a quantidade de Fotos que ainda falta para iniciar o treinamento
    void alertaQtdFotos() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.bt.setText("Reconhecer Usuário");
                MainActivity.tv.setText("Suas Fotos foram capturadas com sucesso," +
                        "Agora já podemos Fazer o Reconhecimento");
                remainigPhotos = Treinamento.FOTOS_TREINAR_QTD - Treinamento.qtdFotos(getBaseContext());
                           if(remainigPhotos == 0) {
                               treinar();
                            }else{
                            Toast.makeText(getBaseContext(), "Precisa de Mais " + remainigPhotos +" Fotos para Iniciar o Treinamento ", Toast.LENGTH_SHORT).show();
                            }

            }
        });
    }

}