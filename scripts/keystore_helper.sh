#!/bin/bash

# Exit on error
set -e

echo "==========================================="
echo "        Android Keystore Helper Tool       "
echo "==========================================="
echo "1. 生成全新 Keystore (Generate new Keystore)"
echo "2. 将已有 Keystore 转换为 Base64 配置信息 (Convert Keystore to Base64)"
echo "3. 将 Base64 配置信息还原为 Keystore 文件 (Restore Keystore from Base64)"
echo "4. 退出 (Exit)"
echo "==========================================="
read -p "请选择操作 [1-4]: " CHOICE

case "$CHOICE" in
    1)
        echo ""
        echo "--> 开始生成全新 Keystore..."
        read -p "请输入 Keystore 文件名 (默认: release.keystore): " KEYSTORE_NAME
        KEYSTORE_NAME=${KEYSTORE_NAME:-release.keystore}

        read -p "请输入证书别名 Alias (默认: release-key): " ALIAS
        ALIAS=${ALIAS:-release-key}

        read -s -p "请输入密码 (至少6位): " PASSWORD
        echo ""
        read -s -p "请再次输入密码以确认: " PASSWORD_CONFIRM
        echo ""

        if [ "$PASSWORD" != "$PASSWORD_CONFIRM" ]; then
            echo "错误: 两次输入的密码不一致！"
            exit 1
        fi

        if [ ${#PASSWORD} -lt 6 ]; then
            echo "错误: 密码长度必须至少为6位！"
            exit 1
        fi

        echo "正在生成 Keystore 文件 $KEYSTORE_NAME ..."
        
        # 使用 keytool 生成证书
        keytool -genkeypair -v \
          -keystore "$KEYSTORE_NAME" \
          -alias "$ALIAS" \
          -keyalg RSA \
          -keysize 2048 \
          -validity 10000 \
          -storepass "$PASSWORD" \
          -keypass "$PASSWORD" \
          -dname "CN=KMPApp, OU=Development, O=KMPOrg, L=Beijing, S=Beijing, C=CN"

        echo ""
        echo "====================================================="
        echo "✅ Keystore 生成成功!"
        echo "文件路径: $(pwd)/$KEYSTORE_NAME"
        echo "证书别名 (Alias): $ALIAS"
        echo "存储密码 (Store Password) 和密钥密码 (Key Password): ******"
        echo "====================================================="
        ;;
        
    2)
        echo ""
        echo "--> 开始转换 Keystore 为 Base64..."
        read -p "请输入已有的 Keystore 文件路径: " FILE_PATH

        # 去除用户可能拖拽文件带入的首尾空格或引号
        FILE_PATH=$(echo "$FILE_PATH" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^["'\''\\]*//' -e 's/["'\''\\]*$//')

        if [ ! -f "$FILE_PATH" ]; then
            echo "错误: 文件 '$FILE_PATH' 不存在！"
            exit 1
        fi

        echo "正在转换 $FILE_PATH ..."
        
        # 判断操作系统以使用正确的 base64 命令
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            base64 -i "$FILE_PATH" -o "${FILE_PATH}.base64.txt"
        else
            # Linux / Git Bash
            base64 -w 0 "$FILE_PATH" > "${FILE_PATH}.base64.txt"
        fi

        echo ""
        echo "====================================================="
        echo "✅ 转换完成!"
        echo "Base64 字符串已保存至: ${FILE_PATH}.base64.txt"
        echo "您可以打开该文件，复制其中的所有内容并填入 GitHub Secrets 中的 ANDROID_KEYSTORE_BASE64。"
        echo "====================================================="
        ;;

    3)
        echo ""
        echo "--> 开始将 Base64 还原为 Keystore..."
        read -p "请输入 Base64 文本文件路径: " FILE_PATH

        # 去除首尾空格或引号
        FILE_PATH=$(echo "$FILE_PATH" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^["'\''\\]*//' -e 's/["'\''\\]*$//')

        if [ ! -f "$FILE_PATH" ]; then
            echo "错误: 文件 '$FILE_PATH' 不存在！"
            exit 1
        fi

        read -p "请输入要保存的 Keystore 文件名 (默认: restored.keystore): " OUT_PATH
        OUT_PATH=${OUT_PATH:-restored.keystore}

        echo "正在解码还原至 $OUT_PATH ..."

        # 判断操作系统以使用正确的 base64 解码命令
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            base64 -D -i "$FILE_PATH" -o "$OUT_PATH"
        else
            # Linux / Git Bash
            base64 -d "$FILE_PATH" > "$OUT_PATH"
        fi

        echo ""
        echo "====================================================="
        echo "✅ 还原完成!"
        echo "Keystore 文件已保存至: $(pwd)/$OUT_PATH"
        echo "====================================================="
        ;;
        
    4)
        echo "已退出。"
        exit 0
        ;;
        
    *)
        echo "无效的选项。"
        exit 1
        ;;
esac
