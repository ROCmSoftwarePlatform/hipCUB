/******************************************************************************
 * Copyright (c) 2011, Duane Merrill.  All rights reserved.
 * Copyright (c) 2011-2018, NVIDIA CORPORATION.  All rights reserved.
 * Modifications Copyright (c) 2017-2021, Advanced Micro Devices, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the NVIDIA CORPORATION nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NVIDIA CORPORATION BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ******************************************************************************/


#include "hipcub/thread/thread_load.hpp"

#include "common_test_header.hpp"

template<class T>
struct params
{
    using type = T;
};

template<class Params>
class HipcubThreadOperationTests : public ::testing::Test
{
public:
    using type = typename Params::type;
};

// TODO add custom types larger than 64 bits 
typedef ::testing::Types<
    params<uint8_t>,
    params<uint16_t>,
    params<uint32_t>,
    params<uint64_t>,
    params<test_utils::custom_test_type<uint64_t>>,
    params<test_utils::custom_test_type<double>>
> ThreadOperationTestParams;

template<class Type>
__global__
void thread_load_kernel(Type* const device_input, Type* device_output)
{
    size_t index = hipBlockIdx_x * hipBlockDim_x + hipThreadIdx_x;
    device_output[index] = hipcub::ThreadLoad<hipcub::LOAD_CG>(device_input + index); 
}

TYPED_TEST_CASE(HipcubThreadOperationTests, ThreadOperationTestParams);

TYPED_TEST(HipcubThreadOperationTests, Load)
{
    using T = typename TestFixture::type;
    constexpr uint32_t block_size = 256;
    constexpr uint32_t grid_size = 1;
    constexpr uint32_t size = 256;

    for (size_t seed_index = 0; seed_index < random_seeds_count + seed_size; seed_index++)
    {
        unsigned int seed_value = seed_index < random_seeds_count  ? rand() : seeds[seed_index - random_seeds_count];
        SCOPED_TRACE(testing::Message() << "with seed= " << seed_value);

        // Generate data
        std::vector<T> input = test_utils::get_random_data<T>(size, 2, 200, seed_value);
        std::vector<T> output(size);

        // Calculate expected results on host
        std::vector<T> expected = input;

        // Preparing device
        T* device_input;
        HIP_CHECK(hipMalloc(&device_input, input.size() * sizeof(T)));
        T* device_output;
        HIP_CHECK(hipMalloc(&device_output, output.size() * sizeof(T)));

        HIP_CHECK(
            hipMemcpy(
                device_input, input.data(),
                input.size() * sizeof(T),
                hipMemcpyHostToDevice
            )
        );

        thread_load_kernel<T><<<grid_size, block_size>>>(device_input, device_output);

        // Reading results back
        HIP_CHECK(
            hipMemcpy(
                output.data(), device_output,
                output.size() * sizeof(T),
                hipMemcpyDeviceToHost
            )
        );

        // Verifying results
        for(size_t i = 0; i < output.size(); i++)
        {
            ASSERT_EQ(output[i], expected[i]);
        }

        HIP_CHECK(hipFree(device_input));
        HIP_CHECK(hipFree(device_output));
    }
}
