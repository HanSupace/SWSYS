const emotions = [
  { id: 'default-happiness', code: 'HAPPY', value: 'happiness', label: '기쁨', icon: '😊', color: '#765A08', background: '#FFF7D7', border: 'rgba(171, 126, 9, 0.22)', positive: true },
  { id: 'default-anticipation', code: 'ANTICIPATION', value: 'anticipation', label: '기대', icon: '🙂', color: '#4F711F', background: '#ECF8DD', border: 'rgba(89, 137, 36, 0.22)', positive: true },
  { id: 'default-sadness', code: 'SAD', value: 'sadness', label: '슬픔', icon: '😢', color: '#315C86', background: '#EAF3FF', border: 'rgba(62, 111, 162, 0.22)', positive: false },
  { id: 'default-anger', code: 'ANGRY', value: 'anger', label: '분노', icon: '😠', color: '#9D312B', background: '#FFF1F0', border: 'rgba(196, 73, 64, 0.22)', positive: false },
  { id: 'default-anxiety', code: 'ANXIOUS', value: 'anxiety', label: '불안', icon: '😟', color: '#5C477D', background: '#F3EEFF', border: 'rgba(106, 78, 150, 0.22)', positive: false },
  { id: 'default-embarrassment', code: 'EMBARRASSED', value: 'embarrassment', label: '당황', icon: '😳', color: '#8B3D66', background: '#FFEFF7', border: 'rgba(177, 75, 120, 0.22)', positive: false },
  { id: 'default-surprise', code: 'SURPRISED', value: 'surprise', label: '놀람', icon: '😮', color: '#28746F', background: '#E8F9F7', border: 'rgba(50, 137, 128, 0.22)', positive: true },
  { id: 'default-irritation', code: 'IRRITATED', value: 'irritation', label: '짜증', icon: '😣', color: '#925021', background: '#FFF0E4', border: 'rgba(181, 99, 31, 0.22)', positive: false }
];

function byLabel(label) {
  return emotions.find((emotion) => emotion.label === String(label || '').trim());
}

module.exports = { emotions, byLabel };
