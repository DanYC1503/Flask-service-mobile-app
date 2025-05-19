import numpy as np
import time
import pycuda.autoinit
import pycuda.driver as cuda
from pycuda.compiler import SourceModule

class InkFilter:
    def __init__(self):
        self.auto_context = pycuda.autoinit.context
        self._mod = None
        self._ink_filter_gpu = None

    def _compile_kernel(self, mask_size):
        kernel_code = f"""
        #define MASK_SIZE {mask_size}
        #define RADIUS (MASK_SIZE / 2)

        __device__ unsigned char luminance(unsigned char r, unsigned char g, unsigned char b) {{
            return (unsigned char)(0.299f * r + 0.587f * g + 0.114f * b);
        }}

        __global__ void ink_filter_gpu(
            unsigned char* input, unsigned char* output,
            int width, int height, int channels)
        {{
            int x = blockIdx.x * blockDim.x + threadIdx.x;
            int y = blockIdx.y * blockDim.y + threadIdx.y;

            if (x >= width || y >= height) return;

            float gx = 0.0f;
            float gy = 0.0f;

            int idx = (y * width + x) * channels;

            for (int ky = -RADIUS; ky <= RADIUS; ky++) {{
                for (int kx = -RADIUS; kx <= RADIUS; kx++) {{
                    int nx = x + kx;
                    int ny = y + ky;

                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {{
                        int neighbor_idx = (ny * width + nx) * channels;
                        unsigned char lum = luminance(
                            input[neighbor_idx],
                            input[neighbor_idx + 1],
                            input[neighbor_idx + 2]
                        );

                        float sobel_x = kx;
                        float sobel_y = ky;

                        gx += sobel_x * lum;
                        gy += sobel_y * lum;
                    }}
                }}
            }}

            float grad = sqrtf(gx * gx + gy * gy);
            unsigned char edge = (grad > 40.0f) ? 255 : 0;

            // Simulación tipo "mancha de tinta"
            unsigned char bead = 0;
            if (edge == 255 && ((x * y) % 7) == 0) {{
                bead = 255;
            }}

            unsigned char val = (edge == 255) ? bead : 255;

            output[idx] = val;
            output[idx + 1] = val;
            output[idx + 2] = val;
        }}
        """
        self._mod = SourceModule(kernel_code)
        self._compiled_mask_size = mask_size
        self._ink_filter_gpu = self._mod.get_function("ink_filter_gpu")

    def process_gpu(self, img_np, mask_size, blocks_x=16, blocks_y=16, threads_x=16, threads_y=16):
        height, width, channels = img_np.shape
        output = np.zeros_like(img_np, dtype=np.uint8)

        block_dim = (threads_x, threads_y, 1)
        grid_dim = (
            (width + threads_x - 1) // threads_x,
            (height + threads_y - 1) // threads_y,
            1
        )

        self.auto_context.push()
        try:
            if self._mod is None:
                self._compile_kernel(mask_size)

            d_input = cuda.mem_alloc(img_np.nbytes)
            d_output = cuda.mem_alloc(output.nbytes)
            cuda.memcpy_htod(d_input, img_np)

            self._ink_filter_gpu(
                d_input, d_output,
                np.int32(width), np.int32(height), np.int32(channels),
                block=block_dim, grid=grid_dim
            )

            cuda.memcpy_dtoh(output, d_output)
        finally:
            self.auto_context.pop()

        return output


# Función principal para usar el filtro
def process_image_ink_filter(img_np, mask_size = 3 ,blocks_x=16, blocks_y=16, threads_x=16, threads_y=16):
    filter = InkFilter()
    start = time.time()

    result = filter.process_gpu(
        img_np,
        mask_size,
        blocks_x=blocks_x,
        blocks_y=blocks_y,
        threads_x=threads_x,
        threads_y=threads_y
    )

    elapsed = time.time() - start
    stats = {
        'time_s': round(elapsed, 4),
        'blocks': f"{blocks_x}x{blocks_y}",
        'threads': f"{threads_x}x{threads_y}",
        'method': 'ink_filter'
    }

    return result, stats
