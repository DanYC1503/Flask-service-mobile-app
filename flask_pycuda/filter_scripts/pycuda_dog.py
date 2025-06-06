import numpy as np
import math
import time
from scipy.signal import convolve2d
import numpy as np
from PIL import Image

import pycuda.autoinit
from pycuda import driver as drv
from pycuda.compiler import SourceModule

# Referencia al contexto creado por autoinit
auto_context = pycuda.autoinit.context

# ====================================================================
# Kernel CUDA (compilarlo una sola vez al cargar el módulo)
_kernel_code = """
__global__ void convolutionGPU(
    unsigned char* input, float* output,
    int width, int height, int channels,
    float* mask, int maskSize)
{
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    
    if (x >= width || y >= height) return;
    
    int offset = maskSize / 2;
    for (int c = 0; c < channels; c++) {
        float sum = 0.0f;
        for (int j = -offset; j <= offset; j++) {
            for (int i = -offset; i <= offset; i++) {
                int xi = x + i;
                int yj = y + j;
                if (xi >= 0 && xi < width && yj >= 0 && yj < height) {
                    int imgIdx = (yj * width + xi) * channels + c;
                    int maskIdx = (j + offset) * maskSize + (i + offset);
                    sum += input[imgIdx] * mask[maskIdx];
                }
            }
        }
        output[(y * width + x) * channels + c] = sum;
    }
}
"""
_mod = SourceModule(_kernel_code)
_convolution_gpu = _mod.get_function("convolutionGPU")

# ====================================================================
# Función para crear la máscara Gaussiana
def create_gaussian_mask(mask_size, sigma):
    offset = mask_size // 2
    mask = np.zeros((mask_size, mask_size), dtype=np.float32)
    sum_val = 0.0
    for y in range(-offset, offset + 1):
        for x in range(-offset, offset + 1):
            v = math.exp(-(x*x + y*y) / (2.0 * sigma * sigma))
            mask[y + offset, x + offset] = v
            sum_val += v
    mask /= sum_val
    return mask


# ====================================================================
# Función principal de procesamiento
def process_image(img_np: np.ndarray, mask_size: int,
                blocks_x=16, blocks_y=16, threads_x=16, threads_y=16):
    h, w, c = img_np.shape

    # Crear máscaras Gaussianas
    mask_small = create_gaussian_mask(mask_size, sigma=1.5)
    mask_large = create_gaussian_mask(mask_size, sigma=10.0)

    # Buffers de salida
    blur1 = np.zeros_like(img_np, dtype=np.float32)
    blur2 = np.zeros_like(img_np, dtype=np.float32)

    stats = {
        'mask_size': mask_size,
    }


    # Configuración GPU personalizada
    block_dim = (threads_x, threads_y, 1)
    grid_dim = (
        (w + threads_x - 1) // threads_x,
        (h + threads_y - 1) // threads_y,
        1
    )
        
    stats.update({
        'blocks': f"{blocks_x}x{blocks_y}",
        'threads': f"{threads_x}x{threads_y}"
    })

    # Asegurar que el contexto de autoinit esté activo
    auto_context.push()
    try:
        # Reservar memoria en GPU
        d_in = drv.mem_alloc(img_np.nbytes)
        d_mask_s = drv.mem_alloc(mask_small.nbytes)
        d_mask_l = drv.mem_alloc(mask_large.nbytes)
        d_b1 = drv.mem_alloc(blur1.nbytes)
        d_b2 = drv.mem_alloc(blur2.nbytes)

        # Copiar datos a GPU
        drv.memcpy_htod(d_in, img_np)
        drv.memcpy_htod(d_mask_s, mask_small)
        drv.memcpy_htod(d_mask_l, mask_large)

        # Lanzar kernels y medir tiempo
        start_evt = drv.Event()
        end_evt = drv.Event()
        start_evt.record()

        _convolution_gpu(
            d_in, d_b1,
            np.int32(w), np.int32(h), np.int32(c),
            d_mask_s, np.int32(mask_size),
            block=block_dim, grid=grid_dim
        )
        _convolution_gpu(
            d_in, d_b2,
            np.int32(w), np.int32(h), np.int32(c),
            d_mask_l, np.int32(mask_size),
            block=block_dim, grid=grid_dim
        )

        end_evt.record()
        end_evt.synchronize()
        stats['time_s'] = end_evt.time_since(start_evt) / 1000.0

        # Copiar resultados de vuelta
        drv.memcpy_dtoh(blur1, d_b1)
        drv.memcpy_dtoh(blur2, d_b2)
    finally:
        auto_context.pop()

    # Generar Difference-of-Gaussian y convertir a uint8
    dog = np.abs(blur1 - blur2)
    dog_u8 = np.clip(dog + 0.5, 0, 255).astype(np.uint8)

    return dog_u8, stats