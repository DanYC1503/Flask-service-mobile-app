import time
from pycuda import driver as cuda
from pycuda.compiler import SourceModule
import numpy as np
from PIL import Image
import mediapipe as mp

# Inicializa el driver manualmente
cuda.init()

def load_image(path):
    img = Image.open(path).convert("RGB")
    return np.array(img)

def resize_to_match(bg_img, target_shape):
    target_h, target_w = target_shape[:2]
    desired_w, desired_h = 1080, 720  # tamaño fijo para el background

    # Resize background siempre a 1080x720
    resized_bg = Image.fromarray(bg_img).resize((desired_w, desired_h), Image.LANCZOS)
    resized_bg_np = np.array(resized_bg)

    # Recortar al centro si es más grande que el target
    if resized_bg_np.shape[0] > target_h or resized_bg_np.shape[1] > target_w:
        start_y = max((resized_bg_np.shape[0] - target_h) // 2, 0)
        start_x = max((resized_bg_np.shape[1] - target_w) // 2, 0)
        resized_bg_np = resized_bg_np[start_y:start_y+target_h, start_x:start_x+target_w]

    return resized_bg_np

def get_person_mask(image_np):
    mp_selfie = mp.solutions.selfie_segmentation
    with mp_selfie.SelfieSegmentation(model_selection=1) as selfie_seg:
        results = selfie_seg.process(image_np)
        mask = (results.segmentation_mask > 0.5).astype(np.uint8) * 255
    return mask

class BackgroundReplacer:
    def __init__(self):
        # Crear y mantener contexto manual
        self.device = cuda.Device(0)
        self.context = self.device.make_context()

        self.mod = SourceModule("""
        __global__ void background_replace(
            unsigned char* input,
            unsigned char* background,
            unsigned char* mask,
            unsigned char* output,
            int width, int height,
            int fg_x_offset,
            int fg_y_offset)
        {
            int x = blockIdx.x * blockDim.x + threadIdx.x;
            int y = blockIdx.y * blockDim.y + threadIdx.y;

            if (x >= width || y >= height) return;

            // Coordinates inside the input image after offset
            int fg_x = x - fg_x_offset;
            int fg_y = y - fg_y_offset;

            int idx = (y * width + x) * 3;
            int mask_idx;
            bool is_person = false;

            // Check if fg coords are valid inside foreground image bounds
            if (fg_x >= 0 && fg_x < width && fg_y >= 0 && fg_y < height) {
                mask_idx = fg_y * width + fg_x;
                is_person = (mask[mask_idx] > 128);

                if (is_person) {
                    int fg_idx = (fg_y * width + fg_x) * 3;
                    for (int c = 0; c < 3; c++) {
                        output[idx + c] = input[fg_idx + c];
                    }
                    return;
                }
            }

            // Else write background pixel
            for (int c = 0; c < 3; c++) {
                output[idx + c] = background[idx + c];
            }
        }


        """)
        self.kernel = self.mod.get_function("background_replace")

    def replace_background(self, img_np, mask_np, background_np,
                       blocks_x=16, blocks_y=16,
                       threads_x=16, threads_y=16,
                       fg_x_offset=0, fg_y_offset=0):
        try:
            self.context.push()
            height, width, _ = img_np.shape

            assert img_np.shape == background_np.shape
            assert mask_np.shape[:2] == img_np.shape[:2]

            mask_flat = mask_np.astype(np.uint8).flatten()
            img_flat = img_np.astype(np.uint8).flatten()
            bg_flat = background_np.astype(np.uint8).flatten()
            output = np.empty_like(img_flat)

            d_input = cuda.mem_alloc(img_flat.nbytes)
            d_mask = cuda.mem_alloc(mask_flat.nbytes)
            d_bg = cuda.mem_alloc(bg_flat.nbytes)
            d_out = cuda.mem_alloc(output.nbytes)

            cuda.memcpy_htod(d_input, img_flat)
            cuda.memcpy_htod(d_mask, mask_flat)
            cuda.memcpy_htod(d_bg, bg_flat)

            block_dim = (threads_x, threads_y, 1)
            grid_dim = (
                (width + threads_x - 1) // threads_x,
                (height + threads_y - 1) // threads_y,
                1
            )

            self.kernel(
                d_input, d_bg, d_mask, d_out,
                np.int32(width), np.int32(height),
                np.int32(fg_x_offset), np.int32(fg_y_offset),
                block=block_dim, grid=grid_dim
            )

            cuda.memcpy_dtoh(output, d_out)
            return output.reshape(img_np.shape)

        finally:
            self.context.pop()


    def __del__(self):
        if hasattr(self, 'context'):
            self.context.detach()
def process_image_background(img_np, mask_np, background_np,
                             blocks_x=16, blocks_y=16,
                             threads_x=16, threads_y=16):
    replacer = BackgroundReplacer()

    height, width, _ = img_np.shape

    # Calculate vertical offset so the person appears bottom-aligned
    fg_y_offset = 0  # if no shift
    # Or push the foreground up by e.g. 1/4 of image height
    fg_y_offset = height // 4  

    # Center horizontally (usually zero offset)
    fg_x_offset = 0

    start = time.time()
    result = replacer.replace_background(
        img_np,
        mask_np,
        background_np,
        blocks_x=blocks_x,
        blocks_y=blocks_y,
        threads_x=threads_x,
        threads_y=threads_y,
        fg_x_offset=fg_x_offset,
        fg_y_offset=fg_y_offset
    )
    elapsed = time.time() - start
    stats = {
        'time_s': round(elapsed, 4),
        'blocks': f"{blocks_x}x{blocks_y}",
        'threads': f"{threads_x}x{threads_y}"
    }
    return result, stats
