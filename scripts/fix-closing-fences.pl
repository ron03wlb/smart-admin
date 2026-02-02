#!/usr/bin/env perl
# IGaming 文檔閉合標記錯誤掃描與修正工具

use strict;
use warnings;
use File::Find;
use File::Basename;
use File::Copy;

my $target_dir = "docs/IGaming";
my $scan_only = $ARGV[0] && $ARGV[0] eq '--scan-only';
my $backup_created = 0;

my %stats = (
    total_files => 0,
    total_errors => 0,
    text => 0,
    yaml => 0,
    markdown => 0,
    sql => 0,
    json => 0,
);

my @error_details;

print "🚀 IGaming 文檔閉合標記錯誤工具\n";
print "=" x 60, "\n";
print "模式: ", $scan_only ? "掃描模式" : "修正模式", "\n";
print "目標: $target_dir\n\n";

# 遍歷所有 .md 文件
find(sub {
    return unless /\.md$/;
    return if /REPORT|CORRECTION|backup/;

    my $file = $File::Find::name;
    process_file($file);
}, $target_dir);

# 輸出統計
print "\n", "=" x 60, "\n";
print "📊 統計結果\n";
print "=" x 60, "\n";
printf "受影響文件: %d\n", $stats{total_files};
printf "總錯誤數: %d\n", $stats{total_errors};

if ($stats{total_errors} > 0) {
    print "\n錯誤類型分布:\n";
    for my $lang (qw(text yaml markdown sql json)) {
        if ($stats{$lang} > 0) {
            my $pct = $stats{$lang} / $stats{total_errors} * 100;
            printf "  - \`\`\`%s: %d 處 (%.1f%%)\n", $lang, $stats{$lang}, $pct;
        }
    }
}

# 輸出詳細錯誤（僅掃描模式）
if ($scan_only && @error_details) {
    print "\n", "=" x 60, "\n";
    print "📁 詳細錯誤清單\n";
    print "=" x 60, "\n\n";

    for my $detail (@error_details) {
        print $detail, "\n";
    }
}

print "\n";
if ($scan_only) {
    print $stats{total_errors} > 0
        ? "⚠️  發現 $stats{total_errors} 處錯誤\n"
        : "✅ 未發現錯誤\n";
} else {
    print $stats{total_errors} > 0
        ? "✅ 已修正 $stats{total_errors} 處錯誤\n"
        : "ℹ️  未發現需要修正的錯誤\n";
}

exit($stats{total_errors} > 0 ? 1 : 0);

# 處理單個文件
sub process_file {
    my ($file) = @_;

    open my $fh, '<', $file or do {
        warn "⚠️  跳過文件（讀取失敗）: $file\n";
        return;
    };
    binmode($fh, ':raw');
    my @lines = <$fh>;
    close $fh;

    my @new_lines;
    my $file_modified = 0;
    my $file_errors = 0;

    for (my $i = 0; $i < @lines; $i++) {
        my $line = $lines[$i];
        my $prev = $i > 0 ? $lines[$i-1] : '';
        my $next = $i < $#lines ? $lines[$i+1] : '';

        # 檢查是否為 ```language 模式
        if ($line =~ /^```(text|yaml|markdown|sql|json)\s*$/) {
            my $lang = $1;

            # 檢查前一行是否非空
            my $prev_trimmed = $prev;
            $prev_trimmed =~ s/^\s+|\s+$//g;

            # 檢查後一行是否為空、分隔符或標題
            my $next_trimmed = $next;
            $next_trimmed =~ s/^\s+|\s+$//g;

            my $is_closing_error = 0;

            if ($prev_trimmed ne '') {
                # 前一行有內容
                if ($next_trimmed eq '' || $next_trimmed =~ /^---/ || $next_trimmed =~ /^#/) {
                    # 後一行為空、分隔符或標題 → 這是閉合標記錯誤
                    $is_closing_error = 1;
                }
            }

            if ($is_closing_error) {
                # 記錄錯誤
                $file_errors++;
                $stats{total_errors}++;
                $stats{$lang}++;

                if (!$file_modified) {
                    $file_modified = 1;
                    $stats{total_files}++;
                }

                # 記錄詳細信息（掃描模式）
                if ($scan_only) {
                    my $rel_path = $file;
                    $rel_path =~ s/^docs\/IGaming\///;

                    my $context = $prev_trimmed;
                    $context = substr($context, 0, 50) . "..." if length($context) > 50;

                    push @error_details, sprintf(
                        "文件: %s\n  Line %d: \`\`\`%s\n  上下文: %s\n",
                        $rel_path, $i + 1, $lang, $context
                    );
                }

                # 修正模式：替換為 ```
                if (!$scan_only) {
                    push @new_lines, "```\n";
                    next;
                }
            }
        }

        push @new_lines, $line;
    }

    # 寫回文件（修正模式且有修改）
    if (!$scan_only && $file_modified) {
        # 創建備份（首次）
        if (!$backup_created) {
            my $timestamp = `date +%Y%m%d-%H%M%S`;
            chomp $timestamp;
            my $backup_dir = "docs/IGaming-backup-$timestamp";

            print "📦 創建備份: $backup_dir\n";
            system("cp -r docs/IGaming '$backup_dir'") == 0
                or die "備份失敗\n";

            $backup_created = 1;
            print "✅ 備份完成\n\n";
        }

        open my $out_fh, '>', $file or die "無法寫入文件: $file\n";
        print $out_fh @new_lines;
        close $out_fh;

        my $rel_path = $file;
        $rel_path =~ s/^docs\/IGaming\///;
        print "✅ 修正: $rel_path ($file_errors 處)\n";
    }
}
