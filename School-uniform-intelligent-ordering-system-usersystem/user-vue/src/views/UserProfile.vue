<template>
  <div class="user-profile-container">
    <el-card class="profile-card">
      <div class="profile-card__header">
        <div>
          <h2>个人信息</h2>
          <p>身高和体重即可用于尺码推荐，胸围等资料为可选增强项。</p>
        </div>
        <el-button plain type="primary" @click="guideVisible = !guideVisible">
          {{ guideVisible ? '收起测量说明' : '如何测量' }}
        </el-button>
      </div>

      <div v-if="guideVisible" class="measurement-guide">
        <div v-for="guide in measurementGuides" :key="guide.key" class="measurement-guide__item">
          <div class="measurement-guide__badge">{{ guide.short }}</div>
          <div>
            <h3>{{ guide.label }}</h3>
            <p>{{ guide.description }}</p>
          </div>
        </div>
      </div>

      <el-form ref="userForm" :model="user" label-width="90px">
        <div class="profile-section">
          <div class="profile-section__title">基础资料</div>
          <p class="profile-section__hint">用于生成尺码推荐，建议保持最新。</p>
        </div>
        <div class="profile-grid">
          <el-form-item label="用户姓名">
            <el-input v-model="user.messageUserName" placeholder="请输入姓名"></el-input>
          </el-form-item>
          <el-form-item label="年龄">
            <el-input-number v-model="user.messageUserAge" :min="1" :max="99" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="user.messageUserSex">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="身高 (cm)">
            <el-input-number v-model="user.height" :min="50" :max="250" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="体重 (kg)">
            <el-input-number v-model="user.weight" :min="10" :max="200" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="profile-section">
          <div class="profile-section__title">可选增强资料</div>
          <p class="profile-section__hint">不填写也可以获得推荐；补充后可帮助判断边界尺码和松紧度。</p>
        </div>
        <div class="profile-grid">
          <el-form-item label="胸围 (cm)">
            <el-input-number v-model="user.chest" :min="50" :max="150" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="腰围 (cm)">
            <el-input-number v-model="user.waist" :min="45" :max="150" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="臀围 (cm)">
            <el-input-number v-model="user.hip" :min="60" :max="170" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="肩宽 (cm)">
            <el-input-number v-model="user.shoulder" :min="25" :max="70" :precision="1" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="profile-section">
          <div class="profile-section__title">学校信息</div>
        </div>
        <div class="profile-grid">
          <el-form-item label="学校" prop="schoolId">
            <el-select
              v-model="user.schoolId"
              placeholder="请选择学校"
              @change="handleUserSchoolChange"
              clearable
              filterable
              :loading="loadingSchoolOptions"
              style="width: 100%;"
            >
              <el-option
                v-for="school in schoolOptions"
                :key="school.schoolId"
                :label="school.schoolName"
                :value="school.schoolId"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="年级" prop="gradeId">
            <el-select
              v-model="user.gradeId"
              placeholder="请选择年级"
              clearable
              filterable
              :disabled="!user.schoolId || loadingGradeOptions || gradeOptions.length === 0"
              :loading="loadingGradeOptions"
              style="width: 100%;"
            >
              <el-option
                v-for="grade in gradeOptions"
                :key="grade.gradeId"
                :label="grade.gradeName"
                :value="grade.gradeId"
              ></el-option>
            </el-select>
          </el-form-item>
        </div>
        <el-form-item>
          <el-button type="primary" @click="updateUserInfo" :loading="updatingUserInfo">保存个人信息</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="profile-card address-card">
      <template #header>
        <div class="clearfix address-header">
          <span>收货地址管理</span>
          <el-button
            style="float: right; padding: 3px 0"
            type="primary"
            icon="el-icon-plus"
            @click="handleAddAddress"
            size="small"
            :loading="loadingAddresses"
          >
            添加新地址
          </el-button>
        </div>
      </template>

      <div v-if="loadingAddresses" class="loading-text">地址加载中...</div>
      <div v-else-if="addresses.length === 0" class="no-address-text">
        您还没有添加收货地址。
      </div>
      <div v-else class="address-list-profile">
        <div v-for="addr in addresses" :key="addr.id" class="address-item-profile">
          <div class="address-info">
            <div class="info-line">
              <strong class="name">{{ addr.recipientName }}</strong>
              <span class="phone">{{ addr.phoneNumber }}</span>
              <el-tag v-if="addr.isDefault" type="success" size="mini" class="default-tag-profile">默认</el-tag>
            </div>
            <div class="info-line full-address-profile">
              {{ formatFullAddress(addr) }}
            </div>
          </div>
          <div class="address-actions">
            <el-button type="text" @click="handleEditAddress(addr)">编辑</el-button>
            <el-button
              v-if="!addr.isDefault"
              type="text"
              @click="handleSetDefault(addr.id)"
              :loading="settingDefaultId === addr.id"
              class="set-default-btn"
            >
              设为默认
            </el-button>
            <el-button
              type="text"
              @click="handleDeleteAddress(addr.id)"
              :loading="deletingAddressId === addr.id"
              class="delete-btn"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog
      :title="isEditMode ? '编辑收货地址' : '添加新地址'"
      v-model:visible="showAddressDialog"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetAddressForm"
    >
      <el-form :model="currentAddress" :rules="addressFormRules" ref="addressFormRef" label-width="100px">
        <el-form-item label="收货人姓名" prop="recipientName">
          <el-input v-model="currentAddress.recipientName" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item label="手机号码" prop="phoneNumber">
          <el-input v-model="currentAddress.phoneNumber" placeholder="请输入手机号"></el-input>
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="currentAddress.province" placeholder="例如：广东省"></el-input>
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="currentAddress.city" placeholder="例如：深圳市"></el-input>
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="currentAddress.district" placeholder="例如：南山区"></el-input>
        </el-form-item>
        <el-form-item label="详细地址" prop="streetAddress">
          <el-input type="textarea" v-model="currentAddress.streetAddress" placeholder="街道、楼牌号等"></el-input>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="currentAddress.isDefault"></el-switch>
          <span class="default-switch-tip">(设为默认后，之前的默认地址将自动取消)</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDialog">取 消</el-button>
          <el-button type="primary" @click="submitAddressForm" :loading="submittingAddress">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from "../api/api";

export default {
  name: "UserProfile",
  data() {
    const validatePhone = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('请输入手机号码'));
      }
      if (!/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error('手机号码格式不正确'));
      } else {
        callback();
      }
    };
    return {
      guideVisible: false,
      measurementGuides: [
        { key: 'chest', short: '胸', label: '胸围', description: '自然站立，皮尺水平绕过胸部最丰满处一圈，保持贴身但不要勒紧。' },
        { key: 'waist', short: '腰', label: '腰围', description: '在肚脐上方最细处水平测量，呼吸自然，不要刻意收腹。' },
        { key: 'hip', short: '臀', label: '臀围', description: '双脚并拢后，绕过臀部最丰满处测量一圈，保持皮尺平整。' },
        { key: 'shoulder', short: '肩', label: '肩宽', description: '从左肩点量到右肩点，保持上身挺直，皮尺顺着肩线平直展开。' },
      ],
      user: {
        messageUserName: "",
        messageUserAge: null,
        messageUserSex: "",
        height: null,
        weight: null,
        chest: null,
        waist: null,
        hip: null,
        shoulder: null,
        schoolId: null,
        gradeId: null,
      },
      userId: localStorage.getItem("userId"),
      updatingUserInfo: false,
      schoolOptions: [],
      gradeOptions: [],
      loadingSchoolOptions: false,
      loadingGradeOptions: false,
      addresses: [],
      loadingAddresses: true,
      showAddressDialog: false,
      isEditMode: false,
      currentAddress: {
        id: null,
        recipientName: '',
        phoneNumber: '',
        province: '',
        city: '',
        district: '',
        streetAddress: '',
        isDefault: false,
      },
      submittingAddress: false,
      settingDefaultId: null,
      deletingAddressId: null,
      addressFormRules: {
        recipientName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
        phoneNumber: [{ required: true, validator: validatePhone, trigger: 'blur' }],
        province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
        city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
        district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
        streetAddress: [{ required: true, message: '请输入详细街道地址', trigger: 'blur' }],
      },
    };
  },
  methods: {
    async fetchSchoolOptionsForUser() {
      this.loadingSchoolOptions = true;
      try {
        const response = await api.getSchoolOptions();
        this.schoolOptions = response?.data || [];
      } catch (error) {
        console.error("获取学校列表失败 (用户):", error);
        this.$message.error("获取学校选项失败");
      } finally {
        this.loadingSchoolOptions = false;
      }
    },
    async fetchGradeOptionsForUser(schoolId) {
      if (!schoolId) {
        this.gradeOptions = [];
        this.user.gradeId = null;
        return;
      }
      this.loadingGradeOptions = true;
      try {
        const response = await api.getGradeOptionsBySchool(schoolId);
        this.gradeOptions = response?.data || [];
      } catch (error) {
        console.error("获取年级列表失败 (用户):", error);
        this.$message.error("获取年级选项失败");
        this.gradeOptions = [];
        this.user.gradeId = null;
      } finally {
        this.loadingGradeOptions = false;
      }
    },
    async handleUserSchoolChange(schoolId) {
      this.user.gradeId = null;
      this.gradeOptions = [];
      if (schoolId) {
        await this.fetchGradeOptionsForUser(schoolId);
      }
    },
    applyUserMessage(data = {}) {
      this.user.messageUserName = data.messageUserName || '';
      this.user.messageUserAge = data.messageUserAge ?? null;
      this.user.messageUserSex = data.messageUserSex || '';
      this.user.height = data.height ?? null;
      this.user.weight = data.weight ?? null;
      this.user.chest = data.chest ?? null;
      this.user.waist = data.waist ?? null;
      this.user.hip = data.hip ?? null;
      this.user.shoulder = data.shoulder ?? null;
      this.user.schoolId = data.schoolId || null;
      this.user.gradeId = data.gradeId || null;
    },
    async fetchUserInfo() {
      if (!this.userId) {
        this.$message.error("未登录");
        this.$router.push("/login");
        return;
      }
      try {
        const response = await api.getUserMessage(this.userId);
        this.applyUserMessage(response.data || {});
        if (this.user.schoolId) {
          await this.fetchGradeOptionsForUser(this.user.schoolId);
        }
      } catch (error) {
        if (error.response?.status === 404) {
          this.applyUserMessage();
          return;
        }
        console.error("获取用户信息失败:", error);
        this.$message.error("获取用户信息失败，请稍后重试");
      }
    },
    async updateUserInfo() {
      if (!this.userId) {
        this.$message.error("未登录");
        return;
      }
      this.updatingUserInfo = true;
      try {
        const payload = {
          messageUserName: this.user.messageUserName,
          messageUserAge: this.user.messageUserAge,
          messageUserSex: this.user.messageUserSex,
          height: this.user.height,
          weight: this.user.weight,
          chest: this.user.chest,
          waist: this.user.waist,
          hip: this.user.hip,
          shoulder: this.user.shoulder,
          schoolId: this.user.schoolId,
          gradeId: this.user.gradeId,
          messageUserId: this.userId,
          updateBy: this.userId,
        };
        const response = await api.updateUserMessage(payload);
        if (response && (response.data === "更新成功" || response.status === 200)) {
          this.$message.success("个人信息已更新");
          await this.fetchUserInfo();
        } else {
          throw new Error(response?.data?.message || "更新失败");
        }
      } catch (error) {
        console.error("更新个人信息失败:", error);
        this.$message.error(error.response?.data || error.message || "更新失败，请稍后再试");
      } finally {
        this.updatingUserInfo = false;
      }
    },
    async fetchAddresses() {
      this.loadingAddresses = true;
      try {
        const response = await api.listAddresses();
        this.addresses = response.data || [];
      } catch (error) {
        console.error("获取地址列表失败:", error);
        this.$message.error(error.response?.data?.message || "获取地址列表失败");
        this.addresses = [];
      } finally {
        this.loadingAddresses = false;
      }
    },
    formatFullAddress(address) {
      if (!address) return '';
      return `${address.province || ''} ${address.city || ''} ${address.district || ''} ${address.streetAddress || ''}`;
    },
    resetAddressForm() {
      this.currentAddress = {
        id: null,
        recipientName: '',
        phoneNumber: '',
        province: '',
        city: '',
        district: '',
        streetAddress: '',
        isDefault: false,
      };
      this.$refs.addressFormRef?.resetFields();
    },
    closeDialog() {
      this.showAddressDialog = false;
    },
    handleAddAddress() {
      this.isEditMode = false;
      this.resetAddressForm();
      this.showAddressDialog = true;
    },
    handleEditAddress(address) {
      this.isEditMode = true;
      this.currentAddress = { ...address };
      this.showAddressDialog = true;
    },
    submitAddressForm() {
      this.$refs.addressFormRef.validate(async (valid) => {
        if (!valid) {
          this.$message.error('请检查表单信息是否完整且正确。');
          return false;
        }
        this.submittingAddress = true;
        try {
          if (this.isEditMode) {
            const { id, ...updateData } = this.currentAddress;
            await api.updateAddress(id, updateData);
            this.$message.success("地址更新成功");
          } else {
            const createData = { ...this.currentAddress };
            delete createData.id;
            await api.createAddress(createData);
            this.$message.success("地址添加成功");
          }
          this.closeDialog();
          await this.fetchAddresses();
        } catch (error) {
          console.error("保存地址失败:", error);
          this.$message.error(error.response?.data?.message || "操作失败，请稍后重试");
        } finally {
          this.submittingAddress = false;
        }
      });
    },
    handleSetDefault(addressId) {
      this.$confirm("确定要将这个地址设为默认吗？", "设为默认", {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(async () => {
        this.settingDefaultId = addressId;
        try {
          await api.setDefaultAddress(addressId);
          this.$message.success("默认地址设置成功");
          await this.fetchAddresses();
        } catch (error) {
          console.error("设置默认地址失败:", error);
          this.$message.error(error.response?.data?.message || "设置默认地址失败");
        } finally {
          this.settingDefaultId = null;
        }
      }).catch(() => {
        this.$message.info("操作已取消");
      });
    },
    handleDeleteAddress(addressId) {
      this.$confirm("确定要删除这个地址吗？此操作不可恢复。", "删除地址", {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.deletingAddressId = addressId;
        try {
          await api.deleteAddress(addressId);
          this.$message.success("地址删除成功");
          await this.fetchAddresses();
        } catch (error) {
          console.error("删除地址失败:", error);
          this.$message.error(error.response?.data?.message || "删除地址失败");
        } finally {
          this.deletingAddressId = null;
        }
      }).catch(() => {
        this.$message.info("操作已取消");
      });
    }
  },
  async mounted() {
    if (!this.userId) {
      this.$message.error("用户未登录，请先登录");
      this.$router.push("/login");
      return;
    }
    await this.fetchSchoolOptionsForUser();
    await this.fetchUserInfo();
    this.fetchAddresses();
  },
};
</script>

<style scoped>
.user-profile-container {
  max-width: 900px;
  margin: 24px auto 48px;
  padding: 20px;
}

.profile-card {
  padding: 18px 26px 26px 26px;
  margin-bottom: 30px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.profile-card__header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.profile-card__header h2 {
  margin: 0 0 6px;
  color: var(--text-primary);
}

.profile-card__header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.5;
}

.measurement-guide {
  display: grid;
  gap: 12px;
  margin-bottom: 20px;
}

.measurement-guide__item {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
  border: 1px solid var(--line);
}

.measurement-guide__badge {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 700;
}

.measurement-guide__item h3 {
  margin: 0 0 6px;
  color: var(--text-primary);
}

.measurement-guide__item p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.profile-section {
  grid-column: 1 / -1;
  margin: 4px 0 14px;
}

.profile-section__title {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.profile-section__hint {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.el-form-item {
  margin-bottom: 18px;
}

.address-card .el-card__header {
  padding: 15px 25px;
  border-bottom: 1px solid var(--line);
}

.address-card .clearfix span {
  font-weight: bold;
  font-size: 16px;
  color: var(--text-primary);
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.address-list-profile {
  margin-top: 10px;
}

.address-item-profile {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
}

.address-item-profile:last-child {
  border-bottom: none;
}

.address-info {
  flex-grow: 1;
  margin-right: 20px;
}

.info-line {
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-line .name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.info-line .phone {
  font-size: 14px;
  color: var(--text-secondary);
}

.default-tag-profile {
  height: 20px;
  line-height: 18px;
  padding: 0 6px;
}

.full-address-profile {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
  display: block;
  align-items: initial;
  gap: initial;
}

.address-actions {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
}

.address-actions .el-button--text {
  padding: 0;
  font-size: 14px;
}

.delete-btn {
  color: var(--danger);
}

.delete-btn:hover {
  color: var(--danger);
}

.set-default-btn {
  color: var(--warning);
}

.set-default-btn:hover {
  color: var(--warning);
}

.loading-text,
.no-address-text {
  text-align: center;
  color: var(--text-secondary);
  padding: 20px;
}

.default-switch-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 10px;
}

.dialog-footer {
  text-align: right;
}

@media (max-width: 768px) {
  .profile-card__header,
  .profile-grid,
  .address-item-profile {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .profile-grid {
    display: grid;
  }
}
</style>
