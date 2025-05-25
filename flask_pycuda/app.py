import os
from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
from filter_scripts.pycuda_dog import process_image
from filter_scripts.pycuda_motion_blur import process_image_motion_blur
from filter_scripts.pycuda_mean_filter import process_image_mean_filter
from filter_scripts.pycuda_negative import process_image_negative
from filter_scripts.pycuda_ink import process_image_ink_filter
from filter_scripts.pycuda_don_bosco import process_image_background, load_image, resize_to_match, get_person_mask
from PIL import Image
import numpy as np
import base64
from io import BytesIO
import mediapipe as mp

UPLOAD_FOLDER = 'static/uploads'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg'}

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/process', methods=['POST'])
def process():
    file = request.files.get('image')
    print("Recibido archivo:", file)
    if not file or not allowed_file(file.filename):
        return jsonify({'error': 'Formato de imagen no válido'}), 400

    filename = secure_filename(file.filename)
    path_in = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    file.save(path_in)

    # Leer imagen como numpy array
    img = Image.open(path_in).convert('RGB')
    img_np = np.array(img, dtype=np.uint8)

    # Tipo de procesamiento
    method = request.form.get('method')

    # Leer tamaño de máscara personalizado
    try:
        mask_size = int(request.form['mask_size'])
        if mask_size < 1 or mask_size > 501 or mask_size % 2 == 0:
            raise ValueError
    except (KeyError, ValueError):
        return jsonify({'error': 'Tamaño de máscara inválido: debe ser impar entre 1 y 501'}), 400

    # Configuración fija de GPU
    gpu_config = {
        'blocks_x': 16,
        'blocks_y': 16,
        'threads_x': 16,
        'threads_y': 16
    }

    # Procesamiento según método
    try:
        if method == 'motion':
            result_np, stats = process_image_motion_blur(img_np, mask_size, **gpu_config)
            out_name = f"motion_gpu_{mask_size}.jpg"
        elif method == 'mean':
            result_np, stats = process_image_mean_filter(img_np, mask_size, **gpu_config)
            out_name = f"mean_gpu_{mask_size}.jpg"
        elif method == 'negative':
            result_np, stats = process_image_negative(img_np, **gpu_config)
            out_name = f"negative_gpu.jpg"
        elif method == 'ink':
            result_np, stats = process_image_ink_filter(img_np, mask_size, **gpu_config)
            out_name = f"ink_gpu.jpg"
        elif method == 'back':
            bg_np = load_image("static/BackGrounds/background.jpg")
            bg_np = resize_to_match(bg_np, img_np.shape)
            mask_np = get_person_mask(img_np)
            result_np, stats = process_image_background(img_np, mask_np, bg_np)
            out_name = f"backGround_filter_gpu.jpg"

        else:  # default: dog
            result_np, stats = process_image(img_np, mask_size, **gpu_config)
            out_name = f"dog_gpu_{mask_size}.jpg"

        stats['method'] = method
        stats.update({
            'mask_size': mask_size,
            'blocks': f"{gpu_config['blocks_x']}x{gpu_config['blocks_y']}",
            'threads': f"{gpu_config['threads_x']}x{gpu_config['threads_y']}"
        })

    except Exception as e:
        return jsonify({'error': f'Error al procesar imagen: {str(e)}'}), 500

    external_output_dir = r"C:\Users\danie\OneDrive\Pictures\flask_filtro"
    os.makedirs(external_output_dir, exist_ok=True)

    path_out = os.path.join(app.config['UPLOAD_FOLDER'], out_name)
    Image.fromarray(result_np).save(path_out)

    # Also save to your OneDrive directory
    external_path = os.path.join(external_output_dir, out_name)
    Image.fromarray(result_np).save(external_path)

    # Generate base64 string
    img_base64 = image_to_json(result_np)

    # Return JSON
    return jsonify({
        'input_image_url': f"/static/uploads/{filename}",
        'output_image_url': os.path.join(external_output_dir, out_name), 
        'external_image_path': external_path,               
        'output_image_base64': img_base64,
        'stats': stats
    }), 200

def image_to_json(result_np):
    img_io = BytesIO()
    Image.fromarray(result_np).save(img_io, format='JPEG')
    img_io.seek(0)
    img_base64 = base64.b64encode(img_io.read()).decode('utf-8')
    return img_base64

if __name__ == '__main__':
    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
    app.run(host='0.0.0.0', debug=True, use_reloader=False)
