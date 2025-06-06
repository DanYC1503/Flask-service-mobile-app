import numpy as np
import time
from PIL import Image
import pycuda.driver as cuda
from pycuda.compiler import SourceModule
cuda.init()

class MeanFilter:
    def __init__(self):
        self.device = cuda.Device(0)
        self.context = self.device.make_context()
        self._mod = None
        self._mean_filter_gpu = None
        self._compiled_mask_size = None

    def _compile_kernel(self, mask_size):
        kernel_code = f"""
        #define FILTER_SIZE {mask_size}
        #define FILTER_RADIUS {mask_size // 2}
        
        __global__ void mean_filter_gpu(
            unsigned char* input, unsigned char* output,
            int width, int height, int channels)
        {{
            int x = blockIdx.x * blockDim.x + threadIdx.x;
            int y = blockIdx.y * blockDim.y + threadIdx.y;

            if (x >= width || y >= height) return;

            for (int c = 0; c < channels; c++) {{
                float sum = 0.0f;
                int count = 0;

                for (int dy = -FILTER_RADIUS; dy <= FILTER_RADIUS; dy++) {{
                    for (int dx = -FILTER_RADIUS; dx <= FILTER_RADIUS; dx++) {{
                        int nx = x + dx;
                        int ny = y + dy;

                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {{
                            int idx = (ny * width + nx) * channels + c;
                            sum += input[idx];
                            count++;
                        }}
                    }}
                }}

                int out_idx = (y * width + x) * channels + c;
                output[out_idx] = (unsigned char)(sum / count);
            }}
        }}
        """
        self._mod = SourceModule(kernel_code)
        self._mean_filter_gpu = self._mod.get_function("mean_filter_gpu")
        self._compiled_mask_size = mask_size

    
    def process_gpu(self, img_np, mask_size, blocks_x=16, blocks_y=16, threads_x=16, threads_y=16):
        height, width, channels = img_np.shape
        output = np.zeros_like(img_np, dtype=np.uint8)

        block_dim = (threads_x, threads_y, 1)
        grid_dim = (
            (width + threads_x - 1) // threads_x,
            (height + threads_y - 1) // threads_y,
            1
        )

        self.context.push()
        try:
            if self._mod is None or self._compiled_mask_size != mask_size:
                self._compile_kernel(mask_size)

            d_input = cuda.mem_alloc(img_np.nbytes)
            d_output = cuda.mem_alloc(output.nbytes)
            cuda.memcpy_htod(d_input, img_np)

            self._mean_filter_gpu(
                d_input, d_output,
                np.int32(width), np.int32(height), np.int32(channels),
                block=block_dim, grid=grid_dim
            )

            cuda.memcpy_dtoh(output, d_output)
        finally:
            self.context.pop()

        return output
    def __del__(self):
        if hasattr(self, 'context'):
            self.context.detach()
# Función principal que usa la clase MeanFilter
def process_image_mean_filter(img_np, mask_size, blocks_x=16, blocks_y=16, threads_x=16, threads_y=16):
    filter = MeanFilter()
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
        'mask_size': mask_size,
        'time_s': round(elapsed, 4)
    }
    
    stats.update({
        'blocks': f"{blocks_x}x{blocks_y}",
        'threads': f"{threads_x}x{threads_y}"
    })

    return result, stats