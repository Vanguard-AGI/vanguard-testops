#!/bin/zsh
# =============================================================================
# spotter-metersphere 重构脚本：从按业务模块拆分 → 按技术层分层
# 使用 zsh（macOS 默认 shell）
# =============================================================================
set -e

PROJECT_ROOT="/Users/jan/IdeaProjects/spotter-metersphere"
cd "$PROJECT_ROOT"

# =============================================================================
# 辅助函数
# =============================================================================
move_dir() {
  local src="$1"
  local dst="$2"
  if [ -d "$src" ] && [ "$(ls -A "$src" 2>/dev/null)" ]; then
    mkdir -p "$dst"
    cp -R "$src"/* "$dst/" 2>/dev/null || true
    rm -rf "$src"
    echo "  ✓ $src → $dst"
  fi
}

# =============================================================================
# 定义模块 → 包 的映射（用两个数组平行对应）
# =============================================================================
MODULES=(
  spotter-metersphere-api-test
  spotter-metersphere-bug-management
  spotter-metersphere-case-management
  spotter-metersphere-dashboard
  spotter-metersphere-metadata-management
  spotter-metersphere-project-management
  spotter-metersphere-requirement-quality
  spotter-metersphere-system-setting
  spotter-metersphere-test-plan
  spotter-metersphere-workflow-management
)

PACKAGES=(
  io/metersphere/api
  io/metersphere/bug
  io/metersphere/functional
  io/metersphere/dashboard
  io/metersphere/metadata
  io/metersphere/project
  io/metersphere/requirementquality
  io/metersphere/system
  io/metersphere/plan
  io/metersphere/workflow
)

echo "=========================================="
echo "Phase 4a: Migrating infrastructure from system-setting..."
echo "=========================================="

SYS_SRC="spotter-metersphere-system-setting/src/main/java/io/metersphere/system"
INFRA_DST="spotter-metersphere-infrastructure/src/main/java/io/metersphere/system"

for infra_dir in security uid file notice schedule log config interceptor serializer resolver; do
  move_dir "$SYS_SRC/$infra_dir" "$INFRA_DST/$infra_dir"
done

# system-setting resources → infrastructure
SYS_RES="spotter-metersphere-system-setting/src/main/resources"
INFRA_RES="spotter-metersphere-infrastructure/src/main/resources"
if [ -d "$SYS_RES" ]; then
  for item in "$SYS_RES"/*; do
    if [ -e "$item" ]; then
      bname=$(basename "$item")
      if [ "$bname" != "mapper" ]; then
        if [ ! -e "$INFRA_RES/$bname" ]; then
          mv "$item" "$INFRA_RES/$bname" 2>/dev/null || true
          echo "  ✓ system resource $bname → infrastructure"
        fi
      fi
    fi
  done
fi

echo ""
echo "=========================================="
echo "Phase 4b: Migrating controllers → web..."
echo "=========================================="

for i in {1..${#MODULES[@]}}; do
  module="${MODULES[$i]}"
  pkg="${PACKAGES[$i]}"
  src="$module/src/main/java/$pkg/controller"
  dst="spotter-metersphere-web/src/main/java/$pkg/controller"
  move_dir "$src" "$dst"
done

echo ""
echo "=========================================="
echo "Phase 4c: Migrating services → service module..."
echo "=========================================="

for i in {1..${#MODULES[@]}}; do
  module="${MODULES[$i]}"
  pkg="${PACKAGES[$i]}"
  src="$module/src/main/java/$pkg/service"
  dst="spotter-metersphere-service/src/main/java/$pkg/service"
  move_dir "$src" "$dst"
done

echo ""
echo "=========================================="
echo "Phase 4d: Migrating extended mappers → dao..."
echo "=========================================="

for i in {1..${#MODULES[@]}}; do
  module="${MODULES[$i]}"
  pkg="${PACKAGES[$i]}"

  # Java mapper files - merge into dao's existing mapper directory
  src="$module/src/main/java/$pkg/mapper"
  dst="spotter-metersphere-dao/src/main/java/$pkg/mapper"
  if [ -d "$src" ] && [ "$(ls -A "$src" 2>/dev/null)" ]; then
    mkdir -p "$dst"
    for f in "$src"/*; do
      if [ -f "$f" ]; then
        fname=$(basename "$f")
        if [ ! -f "$dst/$fname" ]; then
          cp "$f" "$dst/$fname"
          rm "$f"
        else
          echo "  ⚠ Skip $fname (exists in dao)"
        fi
      elif [ -d "$f" ]; then
        dname=$(basename "$f")
        cp -R "$f" "$dst/$dname" 2>/dev/null || true
        rm -rf "$f"
      fi
    done
    echo "  ✓ Merged mappers from $module"
  fi

  # XML mapper files
  xml_src="$module/src/main/resources/mapper"
  xml_dst="spotter-metersphere-dao/src/main/java/io/metersphere/workflow/mapper"
  if [ -d "$xml_src" ] && [ "$(ls -A "$xml_src" 2>/dev/null)" ]; then
    mkdir -p "$xml_dst"
    for f in "$xml_src"/*.xml; do
      if [ -f "$f" ]; then
        fname=$(basename "$f")
        if [ ! -f "$xml_dst/$fname" ]; then
          cp "$f" "$xml_dst/$fname"
          rm "$f"
        else
          echo "  ⚠ Skip XML $fname (exists in dao)"
        fi
      fi
    done
    echo "  ✓ Merged mapper XMLs from $module"
  fi
done

echo ""
echo "=========================================="
echo "Phase 4e: Migrating DTOs, constants → service..."
echo "=========================================="

for i in {1..${#MODULES[@]}}; do
  module="${MODULES[$i]}"
  pkg="${PACKAGES[$i]}"
  base_src="$module/src/main/java/$pkg"
  base_dst="spotter-metersphere-service/src/main/java/$pkg"

  for sub_dir in dto request response constants enums result domain config; do
    move_dir "$base_src/$sub_dir" "$base_dst/$sub_dir"
  done
done

echo ""
echo "=========================================="
echo "Phase 4f: Migrating remaining code → service..."
echo "=========================================="

for i in {1..${#MODULES[@]}}; do
  module="${MODULES[$i]}"
  pkg="${PACKAGES[$i]}"
  base_src="$module/src/main/java/$pkg"
  base_dst="spotter-metersphere-service/src/main/java/$pkg"

  for sub_dir in job listener handler parser excel xmind socket invoker utils provider api manager; do
    move_dir "$base_src/$sub_dir" "$base_dst/$sub_dir"
  done

  # Move any remaining top-level Java files
  if [ -d "$base_src" ]; then
    for f in "$base_src"/*.java(N); do
      if [ -f "$f" ]; then
        mkdir -p "$base_dst"
        mv "$f" "$base_dst/"
        echo "  ✓ Moved top-level $(basename "$f") from $module"
      fi
    done
  fi
done

echo ""
echo "=========================================="
echo "Phase 4g: Migrating resources → service..."
echo "=========================================="

for i in {1..${#MODULES[@]}}; do
  module="${MODULES[$i]}"
  res_src="$module/src/main/resources"
  res_dst="spotter-metersphere-service/src/main/resources"

  if [ -d "$res_src" ]; then
    for item in "$res_src"/*; do
      if [ -e "$item" ]; then
        bname=$(basename "$item")
        if [ "$bname" = "mapper" ]; then
          continue
        fi
        if [ ! -e "$res_dst/$bname" ]; then
          mv "$item" "$res_dst/$bname" 2>/dev/null || true
          echo "  ✓ Resource $bname from $module"
        elif [ -d "$item" ] && [ -d "$res_dst/$bname" ]; then
          cp -R "$item"/* "$res_dst/$bname/" 2>/dev/null || true
          rm -rf "$item"
          echo "  ✓ Merged resource dir $bname from $module"
        else
          echo "  ⚠ Skip resource $bname from $module"
        fi
      fi
    done
  fi
done

echo ""
echo "=========================================="
echo "Phase 4h: Merging provider module..."
echo "=========================================="

PROVIDER_SRC="spotter-metersphere-provider/src/main/java/io/metersphere"
COMMON_DST="spotter-metersphere-common/src/main/java/io/metersphere"
SERVICE_DST="spotter-metersphere-service/src/main/java/io/metersphere"

move_dir "$PROVIDER_SRC/dto" "$COMMON_DST/dto"
move_dir "$PROVIDER_SRC/request" "$COMMON_DST/request"
move_dir "$PROVIDER_SRC/provider" "$COMMON_DST/provider"
move_dir "$PROVIDER_SRC/context" "$SERVICE_DST/context"

echo ""
echo "=========================================="
echo "Phase 4i: Migrating test files..."
echo "=========================================="

for i in {1..${#MODULES[@]}}; do
  module="${MODULES[$i]}"
  test_src="$module/src/test/java/io/metersphere"
  test_dst="spotter-metersphere-service/src/test/java/io/metersphere"

  if [ -d "$test_src" ]; then
    mkdir -p "$test_dst"
    for item in "$test_src"/*; do
      if [ -e "$item" ]; then
        bname=$(basename "$item")
        if [ ! -e "$test_dst/$bname" ]; then
          mv "$item" "$test_dst/$bname"
        elif [ -d "$item" ]; then
          cp -R "$item"/* "$test_dst/$bname/" 2>/dev/null || true
          rm -rf "$item"
        fi
      fi
    done
    echo "  ✓ Tests from $module"
  fi

  test_res_src="$module/src/test/resources"
  test_res_dst="spotter-metersphere-service/src/test/resources"
  if [ -d "$test_res_src" ] && [ "$(ls -A "$test_res_src" 2>/dev/null)" ]; then
    mkdir -p "$test_res_dst"
    cp -R "$test_res_src"/* "$test_res_dst/" 2>/dev/null || true
    rm -rf "$test_res_src"/*
    echo "  ✓ Test resources from $module"
  fi
done

echo ""
echo "=========================================="
echo "Phase 5: Cleaning up empty old modules..."
echo "=========================================="

for i in {1..${#MODULES[@]}}; do
  module="${MODULES[$i]}"
  remaining=0
  if [ -d "$module/src" ]; then
    remaining=$(find "$module/src" -type f 2>/dev/null | wc -l | tr -d ' ')
  fi
  if [ "$remaining" -eq 0 ]; then
    echo "  ✓ Removing: $module"
    rm -rf "$module"
  else
    echo "  ⚠ $module still has $remaining files"
    # Show what's left
    find "$module/src" -type f 2>/dev/null | head -5
  fi
done

# provider module
if [ -d "spotter-metersphere-provider/src" ]; then
  prov_rem=$(find "spotter-metersphere-provider/src" -type f 2>/dev/null | wc -l | tr -d ' ')
  if [ "$prov_rem" -eq 0 ]; then
    echo "  ✓ Removing: spotter-metersphere-provider"
    rm -rf spotter-metersphere-provider
  else
    echo "  ⚠ provider still has $prov_rem files"
  fi
fi

echo ""
echo "=========================================="
echo "✅ 文件迁移完成！"
echo "=========================================="
echo ""
echo "当前项目结构:"
ls -d spotter-metersphere-*/  start/ 2>/dev/null
echo ""
echo "文件统计:"
for mod in spotter-metersphere-web spotter-metersphere-service spotter-metersphere-dao spotter-metersphere-common spotter-metersphere-infrastructure spotter-metersphere-plugin; do
  if [ -d "$mod" ]; then
    count=$(find "$mod/src/main/java" -name "*.java" 2>/dev/null | wc -l | tr -d ' ')
    echo "  $mod: $count Java files"
  fi
done
