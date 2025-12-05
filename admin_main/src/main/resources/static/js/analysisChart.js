/*
 * グラフ描画関数
 * @param {string[]} labels - グラフのラベル
 * @param {number[]} dataValues1 - データセット1
 * @param {number[]} dataValues2 - データセット2
 */
function drawAnalysisChart(labels, dataValues1, dataValues2) {
  'use strict'

  const ctx = document.getElementById('myChart');
  const chartWrapper = ctx.closest('.chart-wrapper');

  if (!ctx || !chartWrapper) {
    console.error('Canvas element with id "myChart" or ".chart-wrapper" not found.');
    return;
  }
  
  if (window.myChart instanceof Chart) {
      window.myChart.destroy();
  }

  // --- ★変更点1: プラグインをChart.jsに登録 ---
  // (ChartDataLabels はプラグインファイルによってグローバルに定義されています)
  // ※すでに登録済みの場合は無視されます
  Chart.register(ChartDataLabels);

  // --- グラフのデータ ---
  const chartData = {
    labels: labels,
    datasets: [
      // (データセット1, 2は変更なし)
      {
        label: '(取り出し個数)',
        data: dataValues1,
        backgroundColor: '#007bff',
        borderColor: '#007bff',
        borderWidth: 1
      },
      {
        label: '(期間末在庫数)',
        data: dataValues2,
        backgroundColor: '#dc3545',
        borderColor: '#dc3545',
        borderWidth: 1
      }
    ]
  };

  // --- グラフの作成 ---
  window.myChart = new Chart(ctx, {
    type: 'bar',
    data: chartData,
    options: {
      responsive: true,
      maintainAspectRatio: false, 
	  
	  // 棒の太さを40pxに固定
	  barThickness: 40, 
	
	  
      scales: {
        y: {
          beginAtZero: true,
          // ★推奨: 値がグラフ上部にはみ出ないよう、少し余裕を持たせる
          grace: '10%' // Y軸の最大値に10%の余白を追加
        }
      },
      plugins: {
        legend: { display: true, position: 'top' },
        title: { display: true, text: '工具別 集計' },

        // --- ★変更点2: データラベルプラグイン(datalabels)の設定 ---
        datalabels: {
          anchor: 'end', // 値を棒グラフの終点（上）に配置
          align: 'top',  // 終点より、さらに上側に配置
          color: '#333', // 値の色
          font: {
            weight: 'bold' // フォントを太字に
          },
          // 値が0の場合は表示しない
          display: function(context) {
            return context.dataset.data[context.dataIndex] > 0;
          }
        }
      }
    }
  });

  // --- グラフ幅の自動計算 (変更なし) ---
  const widthPerItem = 150; 
  const numItems = labels.length;
  const calculatedWidth = numItems * widthPerItem;
  const newMinWidth = Math.max(400, calculatedWidth); 
  chartWrapper.style.minWidth = newMinWidth + 'px';
}