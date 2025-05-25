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

mod = SourceModule("""
    __global__ void motion_blur_45(float *img, float *out, float *mask, int width, int height, int channels, int mask_size)
    {
        int x = blockIdx.x * blockDim.x + threadIdx.x;
        int y = blockIdx.y * blockDim.y + threadIdx.y;
        
        if (x < width && y < height)
        {
            int half = mask_size / 2;
            
            for (int c = 0; c < channels; ++c)
            {
                float sum = 0.0;
                for (int k = -half; k <= half; ++k)
                {
                    int new_x = x + k;
                    int new_y = y + k;
                    
                    if (new_x >= 0 && new_x < width && new_y >= 0 && new_y < height)
                    {
                        sum += img[(new_y * width + new_x) * channels + c] * mask[k + half];
                    }
                }
                out[(y * width + x) * channels + c] = sum;
            }
        }
    }
""")

def create_diagonal_kernel(kernel_size):
    kernel = np.zeros((kernel_size, kernel_size), dtype=np.float32)
    for i in range(kernel_size):
        kernel[i, i] = 1.0
    kernel /= kernel_size
    return kernel

def process_image_motion_blur(img_np: np.ndarray, mask_size: int,
                            blocks_x=16, blocks_y=16, threads_x=16, threads_y=16):
    h, w, c = img_np.shape
    stats = {'mask_size': mask_size}

    # Configuración GPU personalizada
    block_dim = (threads_x, threads_y, 1)
    grid_dim = (
        (w + threads_x - 1) // threads_x,
        (h + threads_y - 1) // threads_y,
        1
    )
        
    stats.update({
        'mode': 'GPU',
        'blocks': f"{blocks_x}x{blocks_y}",
        'threads': f"{threads_x}x{threads_y}"
    })

    # Preparar memoria de salida
    output = np.zeros_like(img_np, dtype=np.float32)

    mask_kernel = create_diagonal_kernel(mask_size)
    mask = np.diag(mask_kernel).astype(np.float32)  # extrae solo la diagonal
    # Aplanar imagen para tratarla como float
    flat_img = img_np.astype(np.float32).flatten()
    flat_out = output.flatten()

    auto_context.push()
    try:
        # Asignar memoria GPU
        d_in = drv.mem_alloc(flat_img.nbytes)
        d_out = drv.mem_alloc(flat_out.nbytes)
        d_mask = drv.mem_alloc(mask.nbytes)

        # Copiar datos
        drv.memcpy_htod(d_in, flat_img)
        drv.memcpy_htod(d_mask, mask)
        func = mod.get_function("motion_blur_45")

        # Medir tiempo GPU
        start_evt = drv.Event()
        end_evt = drv.Event()
        start_evt.record()

        # Ejecutar kernel con configuración personalizada
        func(d_in, d_out, d_mask,
            np.int32(w), np.int32(h), np.int32(c), np.int32(mask_size),
            block=block_dim, grid=grid_dim)

        end_evt.record()
        end_evt.synchronize()
        gpu_time = end_evt.time_since(start_evt) / 1000.0
        stats['time_s'] = gpu_time

        # Copiar resultado de vuelta a CPU
        drv.memcpy_dtoh(flat_out, d_out)
        output = flat_out.reshape((h, w, c))

    finally:
        auto_context.pop()

    return np.clip(output, 0, 255).astype(np.uint8), stats